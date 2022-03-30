/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package filemanager.harshapp.hm.fileexplorer.archive;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.exifinterface.media.ExifInterface;
import filemanager.harshapp.hm.fileexplorer.cursor.MatrixCursor;
import filemanager.harshapp.hm.fileexplorer.libcore.io.IoUtils;
import filemanager.harshapp.hm.fileexplorer.misc.CrashReportingManager;
import filemanager.harshapp.hm.fileexplorer.misc.MimePredicate;
import filemanager.harshapp.hm.fileexplorer.misc.ParcelFileDescriptorUtil;
import filemanager.harshapp.hm.fileexplorer.misc.Preconditions;
import filemanager.harshapp.hm.fileexplorer.misc.Utils;
import filemanager.harshapp.hm.fileexplorer.model.DocumentsContract;
import filemanager.harshapp.hm.fileexplorer.model.DocumentsContract.Document;

import static androidx.annotation.RestrictTo.Scope.GROUP_ID;
import static filemanager.harshapp.hm.fileexplorer.misc.MimeTypes.BASIC_MIME_TYPE;

/**
 * Provides basic implementation for creating, extracting and accessing
 * files within archives exposed by a document provider. The id delimiter
 * must be a character which is not used in document ids generated by the
 * document provider.
 *
 * <p>This class is thread safe.
 *
 * @hide
 */
@RestrictTo(GROUP_ID)
public class DocumentArchive implements Closeable {
    private static
    final String TAG = "DocumentArchive";

    private static final String[] DEFAULT_PROJECTION = new String[] {
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_SIZE,
            Document.COLUMN_FLAGS
    };

    private final Context mContext;
    private final String mDocumentId;
    private final char mIdDelimiter;
    private final Uri mNotificationUri;
    private final ZipFile mZipFile;
    private final ExecutorService mExecutor;
    private final Map<String, ZipEntry> mEntries;
    private final Map<String, List<ZipEntry>> mTree;

    private DocumentArchive(
            Context context,
            File file,
            String documentId,
            char idDelimiter,
            @Nullable Uri notificationUri)
            throws IOException {
        mContext = context;
        mDocumentId = documentId;
        mIdDelimiter = idDelimiter;
        mNotificationUri = notificationUri;
        mZipFile = new ZipFile(file);
        mExecutor = Executors.newSingleThreadExecutor();

        // Build the tree structure in memory.
        mTree = new HashMap<String, List<ZipEntry>>();
        mTree.put("/", new ArrayList<ZipEntry>());

        mEntries = new HashMap<String, ZipEntry>();
        ZipEntry entry;
        final List<? extends ZipEntry> entries = Collections.list(mZipFile.entries());
        final Stack<ZipEntry> stack = new Stack<>();
        for (int i = entries.size() - 1; i >= 0; i--) {
            entry = entries.get(i);
            if (entry.isDirectory() != entry.getName().endsWith("/")) {
                throw new IOException(
                        "Directories must have a trailing slash, and files must not.");
            }
            if (mEntries.containsKey(entry.getName())) {
                throw new IOException("Multiple entries with the same name are not supported.");
            }
            mEntries.put(entry.getName(), entry);
            if (entry.isDirectory()) {
                mTree.put(entry.getName(), new ArrayList<ZipEntry>());
            }
            stack.push(entry);
        }

        int delimiterIndex;
        String parentPath;
        ZipEntry parentEntry;
        List<ZipEntry> parentList;

        while (stack.size() > 0) {
            entry = stack.pop();

            delimiterIndex = entry.getName().lastIndexOf('/', entry.isDirectory()
                    ? entry.getName().length() - 2 : entry.getName().length() - 1);
            parentPath =
                    delimiterIndex != -1 ? entry.getName().substring(0, delimiterIndex) + "/" : "/";
            parentList = mTree.get(parentPath);

            if (parentList == null) {
                parentEntry = mEntries.get(parentPath);
                if (parentEntry == null) {
                    // The ZIP file doesn't contain all directories leading to the entry.
                    // It's rare, but can happen in a valid ZIP archive. In such case create a
                    // fake ZipEntry and add it on top of the stack to process it next.
                    parentEntry = new ZipEntry(parentPath);
                    parentEntry.setSize(0);
                    parentEntry.setTime(entry.getTime());
                    mEntries.put(parentPath, parentEntry);
                    stack.push(parentEntry);
                }
                parentList = new ArrayList<ZipEntry>();
                mTree.put(parentPath, parentList);
            }

            parentList.add(entry);
        }
    }

    /**
     * Creates a DocumentsArchive instance for opening, browsing and accessing
     * documents within the archive passed as a local file.
     *
     * @param context Context of the provider.
     * @param file Local file containing the archive.
     * @param documentId ID of the archive document.
     * @param idDelimiter Delimiter for constructing IDs of documents within the archive.
     *            The delimiter must never be used for IDs of other documents.
     * @param notificationUri notificationUri Uri for notifying that the archive file has changed.
     */
    public static DocumentArchive createForLocalFile(
            Context context, File file, String documentId, char idDelimiter,
            @Nullable Uri notificationUri)
            throws IOException {
        return new DocumentArchive(context, file, documentId, idDelimiter, notificationUri);
    }

    /**
     * Creates a DocumentsArchive instance for opening, browsing and accessing
     * documents within the archive passed as a file descriptor.
     *
     * <p>Note, that this method should be used only if the document does not exist
     * on the local storage. A snapshot file will be created, which may be slower
     * and consume significant resources, in contrast to using
     * {@see createForLocalFile(Context, File, String, char, Uri}.
     *
     * @param context Context of the provider.
     * @param descriptor File descriptor for the archive's contents.
     * @param documentId ID of the archive document.
     * @param idDelimiter Delimiter for constructing IDs of documents within the archive.
     *            The delimiter must never be used for IDs of other documents.
     * @param Uri notificationUri Uri for notifying that the archive file has changed.
     * @see createForLocalFile(Context, File, String, char, Uri)
     */
    public static DocumentArchive createForParcelFileDescriptor(
            Context context, ParcelFileDescriptor descriptor, String documentId,
            char idDelimiter, @Nullable Uri notificationUri)
            throws IOException {
        File snapshotFile = null;
        try {
            // Create a copy of the archive, as ZipFile doesn't operate on streams.
            // Moreover, ZipInputStream would be inefficient for large files on
            // pipes.
            snapshotFile = File.createTempFile("android.support.provider.snapshot{",
                    "}.zip", context.getCacheDir());

            try {
                final FileOutputStream outputStream =
                        new ParcelFileDescriptor.AutoCloseOutputStream(
                                ParcelFileDescriptor.open(
                                        snapshotFile, ParcelFileDescriptor.MODE_WRITE_ONLY));
                final ParcelFileDescriptor.AutoCloseInputStream inputStream =
                        new ParcelFileDescriptor.AutoCloseInputStream(descriptor);
                final byte[] buffer = new byte[32 * 1024];
                int bytes;
                while ((bytes = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytes);
                }
                outputStream.flush();
                return new DocumentArchive(context, snapshotFile, documentId, idDelimiter,
                        notificationUri);
            } catch (Exception e){
                CrashReportingManager.logException(e);
                return null;
            }
        } finally {
            // On UNIX the file will be still available for processes which opened it, even
            // after deleting it. Remove it ASAP, as it won't be used by anyone else.
            if (snapshotFile != null) {
                snapshotFile.delete();
            }
        }
    }

    /**
     * Lists child documents of an archive or a directory within an
     * archive. Must be called only for archives with supported mime type,
     * or for documents within archives.
     *
     */
    public Cursor queryChildDocuments(String documentId, @Nullable String[] projection,
            @Nullable String sortOrder) throws FileNotFoundException {
        final ParsedDocumentIdArchive parsedParentId = ParsedDocumentIdArchive.fromDocumentId(
                documentId, mIdDelimiter);
        Preconditions.checkArgumentEquals(mDocumentId, parsedParentId.mArchiveId,
                "Mismatching document ID. Expected: %s, actual: %s.");

        final String parentPath = parsedParentId.mPath != null ? parsedParentId.mPath : "/";
        final MatrixCursor result = new MatrixCursor(
                projection != null ? projection : DEFAULT_PROJECTION);
        if (mNotificationUri != null) {
            result.setNotificationUri(mContext.getContentResolver(), mNotificationUri);
        }

        final List<ZipEntry> parentList = mTree.get(parentPath);
        if (parentList == null) {
            throw new FileNotFoundException();
        }
        for (final ZipEntry entry : parentList) {
            addCursorRow(result, entry);
        }
        return result;
    }

    /**
     * Returns a MIME type of a document within an archive.
     *
     */
    public String getDocumentType(String documentId) throws FileNotFoundException {
        final ParsedDocumentIdArchive parsedId = ParsedDocumentIdArchive.fromDocumentId(
                documentId, mIdDelimiter);
        Preconditions.checkArgumentEquals(mDocumentId, parsedId.mArchiveId,
                "Mismatching document ID. Expected: %s, actual: %s.");
        Preconditions.checkArgumentNotNull(parsedId.mPath, "Not a document within an archive.");

        final ZipEntry entry = mEntries.get(parsedId.mPath);
        if (entry == null) {
            throw new FileNotFoundException();
        }
        return getMimeTypeForEntry(entry);
    }

    /**
     * Returns true if a document within an archive is a child or any descendant of the archive
     * document or another document within the archive.
     *
     */
    public boolean isChildDocument(String parentDocumentId, String documentId) {
        final ParsedDocumentIdArchive parsedParentId = ParsedDocumentIdArchive.fromDocumentId(
                parentDocumentId, mIdDelimiter);
        final ParsedDocumentIdArchive parsedId = ParsedDocumentIdArchive.fromDocumentId(
                documentId, mIdDelimiter);
        Preconditions.checkArgumentEquals(mDocumentId, parsedParentId.mArchiveId,
                "Mismatching document ID. Expected: %s, actual: %s.");
        Preconditions.checkArgumentNotNull(parsedId.mPath,
                "Not a document within an archive.");

        final ZipEntry entry = mEntries.get(parsedId.mPath);
        if (entry == null) {
            return false;
        }

        if (parsedParentId.mPath == null) {
            // No need to compare paths. Every file in the archive is a child of the archive
            // file.
            return true;
        }

        final ZipEntry parentEntry = mEntries.get(parsedParentId.mPath);
        if (parentEntry == null || !parentEntry.isDirectory()) {
            return false;
        }

        final String parentPath = entry.getName();

        // Add a trailing slash even if it's not a directory, so it's easy to check if the
        // entry is a descendant.
        final String pathWithSlash = entry.isDirectory() ? entry.getName() : entry.getName() + "/";
        return pathWithSlash.startsWith(parentPath) && !parentPath.equals(pathWithSlash);
    }

    /**
     * Returns metadata of a document within an archive.
     *
     */
    public Cursor queryDocument(String documentId, @Nullable String[] projection)
            throws FileNotFoundException {
        final ParsedDocumentIdArchive parsedId = ParsedDocumentIdArchive.fromDocumentId(
                documentId, mIdDelimiter);
        Preconditions.checkArgumentEquals(mDocumentId, parsedId.mArchiveId,
                "Mismatching document ID. Expected: %s, actual: %s.");
        Preconditions.checkArgumentNotNull(parsedId.mPath, "Not a document within an archive.");

        final ZipEntry entry = mEntries.get(parsedId.mPath);
        if (entry == null) {
            throw new FileNotFoundException();
        }

        final MatrixCursor result = new MatrixCursor(
                projection != null ? projection : DEFAULT_PROJECTION);
        if (mNotificationUri != null) {
            result.setNotificationUri(mContext.getContentResolver(), mNotificationUri);
        }
        addCursorRow(result, entry);
        return result;
    }

    /**
     * Opens a file within an archive.
     *
     */
    public ParcelFileDescriptor openDocument(
            String documentId, String mode, @Nullable final CancellationSignal signal)
            throws FileNotFoundException {
        Preconditions.checkArgumentEquals("r", mode,
                "Invalid mode. Only reading \"r\" supported, but got: \"%s\".");
        final ParsedDocumentIdArchive parsedId = ParsedDocumentIdArchive.fromDocumentId(
                documentId, mIdDelimiter);
        Preconditions.checkArgumentEquals(mDocumentId, parsedId.mArchiveId,
                "Mismatching document ID. Expected: %s, actual: %s.");
        Preconditions.checkArgumentNotNull(parsedId.mPath, "Not a document within an archive.");

        final ZipEntry entry = mEntries.get(parsedId.mPath);
        if (entry == null) {
            throw new FileNotFoundException();
        }

        InputStream inputStream = null;
        try {
            try {
                inputStream = mZipFile.getInputStream(entry);
            } catch (IOException e) {
                IoUtils.closeQuietly(inputStream);
                // Ideally we'd simply throw IOException to the caller, but for consistency
                // with DocumentsProvider::openDocument, converting it to IllegalStateException.
                throw new IllegalStateException("Failed to open the document.", e);
            }
            if(null == inputStream){
                return null;
            }
            final boolean isWrite = (mode.indexOf('w') != -1);
            if (isWrite) {
                return null;//ParcelFileDescriptorUtil.pipeTo(new UsbFileOutputStream(file));
            } else {
                return ParcelFileDescriptorUtil.pipeFrom(new BufferedInputStream(inputStream));
            }
        } catch (Exception e) {
            CrashReportingManager.logException(e);
            throw new FileNotFoundException("Failed to open document with id " + documentId +
                    " and mode " + mode);
        }
    }

    /**
     * Opens a thumbnail of a file within an archive.
     *
     */
    public AssetFileDescriptor openDocumentThumbnail(
            String documentId, Point sizeHint, final CancellationSignal signal)
            throws FileNotFoundException {
        final ParsedDocumentIdArchive parsedId = ParsedDocumentIdArchive.fromDocumentId(documentId, mIdDelimiter);
//        Preconditions.checkArgumentEquals(mDocumentId, parsedId.mArchiveId,
//                "Mismatching document ID. Expected: %s, actual: %s.");
//        Preconditions.checkArgumentNotNull(parsedId.mPath, "Not a document within an archive.");
//        Preconditions.checkArgument(getDocumentType(documentId).startsWith("image/"),
//                "Thumbnails only supported for image/* MIME type.");

        final ZipEntry entry = mEntries.get(parsedId.mPath);
        if (entry == null) {
            throw new FileNotFoundException();
        }

        if(!Utils.hasKitKat()){
            return new AssetFileDescriptor(openDocument(documentId, "r", signal),
                    0, entry.getSize());
        }

        InputStream inputStream = null;
        try {
            inputStream = mZipFile.getInputStream(entry);
            final ExifInterface exif = new ExifInterface(inputStream);
            if (exif.hasThumbnail()) {
                Bundle extras = null;
                switch (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1)) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        extras = new Bundle(1);
                        extras.putInt(DocumentsContract.EXTRA_ORIENTATION, 90);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        extras = new Bundle(1);
                        extras.putInt(DocumentsContract.EXTRA_ORIENTATION, 180);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        extras = new Bundle(1);
                        extras.putInt(DocumentsContract.EXTRA_ORIENTATION, 270);
                        break;
                }
                final long[] thumb = exif.getThumbnailRange();
                if (thumb != null) {
                    return new AssetFileDescriptor(openDocument(documentId, "r", signal),
                            thumb[0], thumb[1], extras);
                }
            }
        } catch (IOException e) {
            // Ignore the exception, as reading the EXIF may legally fail.
            Log.e(TAG, "Failed to obtain thumbnail from EXIF.", e);
            CrashReportingManager.logException(e);
        } finally {
            IoUtils.closeQuietly(inputStream);
        }

        return new AssetFileDescriptor(
                openDocument(documentId, "r", signal), 0, entry.getSize(), null);
    }

    /**
     * Schedules a gracefully close of the archive after any opened files are closed.
     *
     * <p>This method does not block until shutdown. Once called, other methods should not be
     * called.
     */
    @Override
    public void close() {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                IoUtils.closeQuietly(mZipFile);
            }
        });
        mExecutor.shutdown();
    }

    private void addCursorRow(MatrixCursor cursor, ZipEntry entry) {
        final MatrixCursor.RowBuilder row = cursor.newRow();
        final ParsedDocumentIdArchive parsedId = new ParsedDocumentIdArchive(mDocumentId, entry.getName());
        row.add(Document.COLUMN_DOCUMENT_ID, parsedId.toDocumentId(mIdDelimiter));

        final File file = new File(entry.getName());
        row.add(Document.COLUMN_DISPLAY_NAME, file.getName());
        row.add(Document.COLUMN_SIZE, entry.getSize());
        row.add(Document.COLUMN_PATH, file.getAbsolutePath());

        final String mimeType = getMimeTypeForEntry(entry);
        row.add(Document.COLUMN_MIME_TYPE, mimeType);
        int flags = 0;//mimeType.startsWith("image/") ? Document.FLAG_SUPPORTS_THUMBNAIL : 0;
        if(MimePredicate.mimeMatches(MimePredicate.VISUAL_MIMES, mimeType)){
            flags |= Document.FLAG_SUPPORTS_THUMBNAIL;
        }
        row.add(Document.COLUMN_FLAGS, flags);
    }

    private String getMimeTypeForEntry(ZipEntry entry) {
        if (entry.isDirectory()) {
            return Document.MIME_TYPE_DIR;
        }

        final int lastDot = entry.getName().lastIndexOf('.');
        if (lastDot >= 0) {
            final String extension = entry.getName().substring(lastDot + 1).toLowerCase(Locale.US);
            final String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mimeType != null) {
                return mimeType;
            }
        }

        return BASIC_MIME_TYPE;
    }
}
