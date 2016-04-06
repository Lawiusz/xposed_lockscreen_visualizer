package pl.lawiusz.lockscreenvisualizerxposed;

import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

class PermGrant {
    private static final String PERM_RECORD_AUDIO ="android.permission.RECORD_AUDIO";
    private static final String PERM_MOD_AUDIO ="android.permission.MODIFY_AUDIO_SETTINGS";

    private static final String CLASS_PACKAGE_MANAGER_SERVICE = "com.android.server.pm.PackageManagerService";
    private static final String CLASS_PACKAGE_PARSER_PACKAGE = "android.content.pm.PackageParser.Package";
    public static void initAndroid(final ClassLoader loader) {
        try {
            final Class<?> pmServiceClass = XposedHelpers.findClass(CLASS_PACKAGE_MANAGER_SERVICE, loader);
            XposedHelpers.findAndHookMethod(pmServiceClass, "grantPermissionsLPw",
                    CLASS_PACKAGE_PARSER_PACKAGE, boolean.class, String.class, new XC_MethodHook() {
                        @SuppressWarnings("unchecked")
                        @Override
                        protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                            final String pkgName = (String) XposedHelpers.getObjectField(param.args[0], "packageName");
                            if (pkgName.contentEquals(MainXposedMod.SYSTEMUI_PACKAGE)) {
                                final String mPackage = MainXposedMod.SYSTEMUI_PACKAGE;
                                Object extras = XposedHelpers.getObjectField(param.args[0], "mExtras");
                                Set<String> grantedPerms = (Set<String>) XposedHelpers.getObjectField(extras, "grantedPermissions");
                                Object sharedUser = XposedHelpers.getObjectField(extras, "sharedUser");
                                if(sharedUser != null) grantedPerms = (Set<String>) XposedHelpers.getObjectField(sharedUser, "grantedPermissions");
                                Object settings = XposedHelpers.getObjectField(param.thisObject, "mSettings");
                                Object permissions = XposedHelpers.getObjectField(settings, "mPermissions");

                                // Add android.permission.MODIFY_AUDIO_SETTINGS to be able to use all audio streams.
                                if (!grantedPerms.contains(PERM_MOD_AUDIO)) {
                                    final Object pModifyAudio = XposedHelpers.callMethod(permissions, "get",
                                            PERM_MOD_AUDIO);
                                    grantedPerms.add(PERM_MOD_AUDIO);
                                    int[] gpGids = (int[]) XposedHelpers.getObjectField(extras, "gids");
                                    int[] bpGids = (int[]) XposedHelpers.getObjectField(pModifyAudio, "gids");
                                    //noinspection UnusedAssignment
                                    gpGids = (int[]) XposedHelpers.callStaticMethod(param.thisObject.getClass(),
                                            "appendInts", gpGids, bpGids);
                                    XposedBridge.log("lGranting " + PERM_MOD_AUDIO + " to " + mPackage);
                                }

                                // Add android.permission.RECORD_AUDIOS to be able to capture audio stream.
                                if (!grantedPerms.contains(PERM_RECORD_AUDIO)) {
                                    final Object pRecordAudio = XposedHelpers.callMethod(permissions, "get",
                                            PERM_RECORD_AUDIO);
                                    grantedPerms.add(PERM_RECORD_AUDIO);
                                    int[] gpGids = (int[]) XposedHelpers.getObjectField(extras, "gids");
                                    int[] bpGids = (int[]) XposedHelpers.getObjectField(pRecordAudio, "gids");
                                    XposedHelpers.callStaticMethod(param.thisObject.getClass(),
                                            "appendInts", gpGids, bpGids);
                                    XposedBridge.log("lGranting " + PERM_RECORD_AUDIO + " to " + mPackage);
                                }

                            }
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}
