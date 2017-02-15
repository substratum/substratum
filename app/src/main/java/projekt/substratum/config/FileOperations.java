package projekt.substratum.config;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import projekt.substratum.util.Root;

import static projekt.substratum.config.References.checkMasqueradeJobService;

public class FileOperations {

    private static final String COPY_LOG = "SubstratumCopy";
    private static final String COPYDIR_LOG = "SubstratumCopyDir";
    private static final String CREATE_LOG = "SubstratumCreate";
    private static final String DELETE_LOG = "SubstratumDelete";
    private static final String MOVE_LOG = "SubstratumMove";

    static void adjustContentProvider(final String uri,
                                      final String topic, final String fileName) {
        Root.runCommand("content insert --uri " + uri + " " +
                "--bind name:s:" + topic + " --bind value:s:" + fileName);
    }

    public static void grantPermission(final String packager, final String permission) {
        Root.runCommand("pm grant " + packager + " " + permission);
    }

    public static void setContext(final String foldername) {
        Root.runCommand("chcon -R u:object_r:system_file:s0 " + foldername);
    }

    public static void setPermissions(final int permission, final String foldername) {
        Root.runCommand("chmod " + permission + " " + foldername);
    }

    public static void setPermissionsRecursively(final int permission, final String foldername) {
        Root.runCommand("chmod -R " + permission + " " + foldername);
    }

    static void setProp(final String propName, final String propValue) {
        Root.runCommand("setprop " + propName + " " + propValue);
    }

    public static void symlink(final String source, final String destination) {
        Root.runCommand("ln -s " + source + " " + destination);
    }

    private static String checkMountCMD() {
        Process process = null;
        try {
            Runtime rt = Runtime.getRuntime();
            process = rt.exec(new String[]{"readlink", "/system/bin/mount"});
            try (BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(process.getInputStream()))) {
                return stdInput.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return null;
    }

    public static void mountRW() {
        String mountCMD = checkMountCMD();
        if (mountCMD != null) {
            if (mountCMD.equals("toybox")) {
                Root.runCommand("mount -o rw,remount /system");
            } else if (mountCMD.equals("toolbox")) {
                Root.runCommand("mount -o remount,rw /system");
            }
        }
    }

    public static void mountRWData() {
        String mountCMD = checkMountCMD();
        if (mountCMD != null) {
            if (mountCMD.equals("toybox")) {
                Root.runCommand("mount -o rw,remount /data");
            } else if (mountCMD.equals("toolbox")) {
                Root.runCommand("mount -o remount,rw /data");
            }
        }
    }

    public static void mountRWVendor() {
        String mountCMD = checkMountCMD();
        if (mountCMD != null) {
            if (mountCMD.equals("toybox")) {
                Root.runCommand("mount -o rw,remount /vendor");
            } else if (mountCMD.equals("toolbox")) {
                Root.runCommand("mount -o remount,rw /vendor");
            }
        }
    }

    public static void mountRO() {
        String mountCMD = checkMountCMD();
        if (mountCMD != null) {
            if (mountCMD.equals("toybox")) {
                Root.runCommand("mount -o ro,remount /system");
            } else if (mountCMD.equals("toolbox")) {
                Root.runCommand("mount -o remount,ro /system");
            }
        }
    }

    public static void mountROData() {
        String mountCMD = checkMountCMD();
        if (mountCMD != null) {
            if (mountCMD.equals("toybox")) {
                Root.runCommand("mount -o ro,remount /data");
            } else if (mountCMD.equals("toolbox")) {
                Root.runCommand("mount -o remount,ro /data");
            }
        }
    }

    public static void mountROVendor() {
        String mountCMD = checkMountCMD();
        if (mountCMD != null) {
            if (mountCMD.equals("toybox")) {
                Root.runCommand("mount -o ro,remount /vendor");
            } else if (mountCMD.equals("toolbox")) {
                Root.runCommand("mount -o remount,ro /vendor");
            }
        }
    }

    public static void createNewFolder(String foldername) {
        Log.d(CREATE_LOG, "Using rootless operation to create " + foldername);
        File folder = new File(foldername);
        if (!folder.exists()) {
            Log.d(CREATE_LOG, "Operation " + (folder.mkdirs() ? "succeeded" : "failed"));
            if (!folder.exists()) {
                Log.d(CREATE_LOG, "Using rooted operation to create " + foldername);
                Root.runCommand("mkdir " + foldername);
            }
        } else {
            Log.d("SubstratumCreate", "Folder already exists!");
        }
    }

    public static void copy(Context context, String source, String destination) {
        String dataDir = context.getDataDir().getAbsolutePath();
        String externalDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        boolean needRoot = (!source.startsWith(dataDir) && !source.startsWith(externalDir) &&
                !source.startsWith("/system")) || (!destination.startsWith(dataDir) &&
                !destination.startsWith(externalDir) && !destination.startsWith("/system"));
        if (checkMasqueradeJobService(context) && needRoot) {
            Log.d(COPY_LOG, "Using masquerade rootless operation to copy " + source +
                    " to " + destination);
            MasqueradeService.copy(context, source, destination);

            // Wait until copy succeeds
            File file = new File(destination);
            try {
                int retryCount = 0;
                while (!file.exists() && retryCount < 5) {
                    Thread.sleep(1000);
                    retryCount++;
                }
                if (retryCount == 5) Log.d(COPY_LOG, "Operation timeout");
                Log.d(COPY_LOG, "Operation " + (file.exists() ? "succeeded" : "failed"));
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
        } else {
            copy(source, destination);
        }
    }

    private static void copy(String source, String destination) {
        Log.d(COPY_LOG, "Using rootless operation to copy " + source + " to " +
                destination);
        File in = new File(source);
        File out = new File(destination);
        try {
            FileUtils.copyFile(in, out);
        } catch (IOException e) {
            Log.d(COPY_LOG, "Rootless operation failed, falling back to rooted mode...");
            Root.runCommand("cp -f " + source + " " + destination);
        }
        Log.d(COPY_LOG, "Operation " + (out.exists() ? "succeeded" : "failed"));
    }

    public static void copyDir(Context context, String source, String destination) {
        String dataDir = context.getDataDir().getAbsolutePath();
        String externalDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        boolean needRoot = (!source.startsWith(dataDir) && !source.startsWith(externalDir) &&
                !source.startsWith("/system")) || (!destination.startsWith(dataDir) &&
                !destination.startsWith(externalDir) && !destination.startsWith("/system"));
        if (checkMasqueradeJobService(context) && needRoot) {
            copy(context, source, destination);
        } else {
            copyDir(source, destination);
        }
    }

    private static void copyDir(String source, String destination) {
        Log.d(COPYDIR_LOG, "Using rootless operation to copy " + source + " to " +
                destination);
        File in = new File(source);
        File out = new File(destination);
        try {
            FileUtils.copyDirectory(in, out);
        } catch (IOException e) {
            Log.d(COPYDIR_LOG, "Rootless operation failed, falling back to rooted mode...");
            Root.runCommand("cp -rf " + source + " " + destination);
        }
        Log.d(COPYDIR_LOG, "Operation " + (out.exists() ? "succeeded" : "failed"));
    }

    public static void delete(Context context, String directory) {
        delete(context, directory, true);
    }

    public static void delete(Context context, String directory, boolean deleteParent) {
        String dataDir = context.getDataDir().getAbsolutePath();
        String externalDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        boolean needRoot = (!directory.startsWith(dataDir) && !directory.startsWith(externalDir) &&
                !directory.startsWith("/system"));
        if (checkMasqueradeJobService(context) && needRoot) {
            Log.d(DELETE_LOG, "Using masquerade rootless operation to delete " + directory);
            MasqueradeService.delete(context, directory, deleteParent);

            // Wait until delete success
            File file = new File(directory);
            try {
                int retryCount = 0;
                boolean notDone = (deleteParent && file.exists()) ||
                        (!deleteParent && file.list().length == 0);
                while (notDone && retryCount < 5) {
                    Thread.sleep(1000);
                    retryCount++;
                }
                if (retryCount == 5) Log.d(DELETE_LOG, "Operation timed out");
                Log.d(DELETE_LOG, "Operation " + (!file.exists() ? "succeeded" : "failed"));
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
        } else {
            delete(directory, deleteParent);
        }
    }

    private static void delete(String directory, boolean deleteParent) {
        Log.d(DELETE_LOG, "Using rootless operation to delete " + directory);
        File dir = new File(directory);
        try {
            if (deleteParent) {
                FileUtils.forceDelete(dir);
            } else {
                FileUtils.cleanDirectory(dir);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(DELETE_LOG, "Rootless operation failed, falling back to rooted mode...");
            if (deleteParent) {
                Root.runCommand("rm -rf " + directory);
            } else {
                String command = "rm -rf ";
                if (dir.isDirectory()) {
                    for (File child : dir.listFiles()) {
                        command += child.getAbsolutePath() + " ";
                    }
                    Root.runCommand(command);
                } else {
                    Root.runCommand(command + directory);
                }
            }
        }
        Log.d(DELETE_LOG, "Operation " + (!dir.exists() ? "succeeded" : "failed"));
    }

    public static void move(Context context, String source, String destination) {
        String dataDir = context.getDataDir().getAbsolutePath();
        String externalDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        boolean needRoot = (!source.startsWith(dataDir) && !source.startsWith(externalDir) &&
                !source.startsWith("/system")) || (!destination.startsWith(dataDir) &&
                !destination.startsWith(externalDir) && !destination.startsWith("/system"));
        if (checkMasqueradeJobService(context) && needRoot) {
            Log.d(MOVE_LOG, "Using masquerade rootless operation to move " + source +
                    " to " + destination);
            MasqueradeService.move(context, source, destination);

            // Wait until move success
            File file = new File(destination);
            try {
                int retryCount = 0;
                while (!file.exists() && retryCount < 5) {
                    Thread.sleep(1000);
                    retryCount++;
                }
                if (retryCount == 5) Log.d(MOVE_LOG, "Operation timed out");
                Log.d(MOVE_LOG, "Operation " + (file.exists() ? "succeeded" : "failed"));
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
        } else {
            move(source, destination);
        }
    }

    private static void move(String source, String destination) {
        Log.d(MOVE_LOG, "Using rootless operation to move " + source + " to " +
                destination);
        File in = new File(source);
        File out = new File(destination);
        try {
            if (in.isFile()) {
                FileUtils.moveFile(in, out);
            } else {
                FileUtils.moveDirectory(in, out);
            }
        } catch (IOException e) {
            Log.d(MOVE_LOG, "Rootless operation failed, falling back to rooted mode... ");
            Root.runCommand("mv -f " + source + " " + destination);
        }
        Log.d(MOVE_LOG, "Operation " + (!in.exists() && out.exists() ? "succeeded" : "failed"));
    }
}