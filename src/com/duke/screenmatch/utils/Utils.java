package com.duke.screenmatch.utils;

import com.duke.screenmatch.settings.Settings;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlTag;

import java.awt.*;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;

public class Utils {
    private static String[] ignoreFoldersDefault = {".gradle", "gradle", ".idea", "build", ".git", ".svn"};

    //PropertiesComponent.getInstance().setValue() //保存基本类型及String等
    //PropertiesComponent.getInstance().setValues() //可保存数字

    private static Dimension getDimension() {
        return Toolkit.getDefaultToolkit().getScreenSize();
    }

    public static int getDialogWidth() {
        Dimension dimension = getDimension();
        if (dimension == null) {
            return 400;
        }
        return dimension.width / 4;
    }

    public static int getDialogHeight() {
        Dimension dimension = getDimension();
        if (dimension == null) {
            return 450;
        }
        return dimension.height / 2;
    }

    /**
     * 设置对话框的位置(设置居中)
     */
    public static Point getDialogCenterLocation() {
        Dimension dimension = getDimension();
        Point point = new Point(0, 0);
        if (dimension == null) {
            return point;
        }
        /*
         * dimension.width / 4 对话框的宽度
         * dimension.height / 3 对话框的高度
         */
        point.x = (dimension.width - getDialogWidth()) >> 1;
        point.y = (dimension.height - getDialogHeight()) >> 1;
        return point;
    }

    public static boolean isEmpty(String string) {
        return string == null || "".equals(string.trim());
    }

    public static final String MANIFEST_PATH = "src/main/AndroidManifest.xml";

    public static void searchModules(String projectPath, String basePath, Set<String> ignoreModuleNames, List<String> modules) {
        File baseFile = new File(basePath);
        File[] subDirs = baseFile.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });
        if (subDirs == null || subDirs.length == 0) {
            return;
        }
        for (File subDir : subDirs) {
            // src/main/AndroidManifest.xml
            String subDirPath = subDir.getAbsolutePath();
            File file = Paths.get(subDirPath, MANIFEST_PATH).toFile();
            if (file.exists()) {
                modules.add(subDirPath
                        .replace("\\", "/")
                        .replace(projectPath, ""));
            } else if (!ignoreModuleNames.contains(subDir.getName())) {
                searchModules(projectPath, subDirPath, ignoreModuleNames, modules);
            }
        }
    }

    public static ArrayList<String> getModuleNames(String projectPath) {
        // 格式化分隔符
        projectPath = projectPath.replace("\\", "/");

        // 忽略的目录
        Set<String> ignoreModuleNames = new HashSet<>(Arrays.asList(ignoreFoldersDefault));
        String ignoreModule = Settings.get(projectPath, Settings.KEY_IGNORE_MODULE_NAME);
        if (!isEmpty(ignoreModule)) {
            ignoreModule = ignoreModule.replaceAll("，", ",")
                    .replaceAll("， ", ",")
                    .replaceAll(" ，", ",")
                    .replaceAll(" ， ", ",")
                    .replaceAll(", ", "")
                    .replaceAll(" ,", "")
                    .replaceAll(" , ", "");
        }
        if (!isEmpty(ignoreModule)) {
            String[] readModules = ignoreModule.split(",");
            ignoreModuleNames.addAll(Arrays.asList(readModules));
        }

        ArrayList<String> moduleItems = new ArrayList<>();
        searchModules(projectPath, projectPath, ignoreModuleNames, moduleItems);

        String defaultUsed = Settings.getDefaultModuleName();
        if (defaultUsed != null && !defaultUsed.equals("")) {
            int index = moduleItems.indexOf(defaultUsed);
            if (index >= 0) {
                moduleItems.remove(index);
                moduleItems.add(0, defaultUsed);
            }
        }
        return moduleItems;
    }

    private void setSelectText(AnActionEvent event, String text) {
        if (event == null || Utils.isEmpty(text)) {
            return;
        }
        Project project = event.getData(PlatformDataKeys.PROJECT);
        Editor editor = event.getData(PlatformDataKeys.EDITOR);
        if (editor == null) {
            return;
        }
        Document document = editor.getDocument();
        SelectionModel selectionModel = editor.getSelectionModel();
        final int start = selectionModel.getSelectionStart();
        final int end = selectionModel.getSelectionEnd();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                document.replaceString(start, end, text);
            }
        };
        WriteCommandAction.runWriteCommandAction(project, runnable);
        selectionModel.removeSelection();
    }

    /**
     * 检查或创建子目录
     *
     * @param basePath 父目录路径
     * @param childDir 当前需要创建的目录
     * @return 目录是否存在或创建
     */
    public boolean checkAndCreateChildDir(String basePath, String childDir) {
        if (isEmpty(basePath) || isEmpty(childDir)) {
            return false;
        }
        VirtualFile virtualFile = getVirtualFile(basePath);
        if (virtualFile == null) {
            return false;
        }
        // 判断根目录下是否有childDir文件夹
        VirtualFile dbDir = virtualFile.findChild(childDir);
        if (dbDir == null) {
            // 没有就创建一个
            try {
                virtualFile = virtualFile.createChildDirectory(null, childDir);
                if (virtualFile.isDirectory()
                        && virtualFile.isWritable() && virtualFile.isValid()) {
                    return true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static VirtualFile getVirtualFile(String path) {
        if (isEmpty(path)) {
            return null;
        }
        return LocalFileSystem.getInstance().findFileByPath(path);
    }

    private static PsiFile getPsiFile(VirtualFile virtualFile, Project project) {
        if (virtualFile == null || project == null) {
            return null;
        }
        return PsiManager.getInstance(project).findFile(virtualFile);
    }

    public static String getBasePath(Project project) {
        if (project == null) {
            return null;
        }
        return project.getBasePath() + File.separator;
    }

    /**
     * 获取res目录，包含 layout, values, drawable 或 mipmap
     *
     * @param basePath 工程根目录
     * @return res路径 <br/>
     * 1、module/AndroidManifest.xml <br/>
     * module/res/values <br/>
     * <p>
     * 2、module/src/main/AndroidManifest.xml <br/>
     * module/src/main/res/values <br/>
     */
    public static String getResPath(String basePath, String moduleName) {
        if (isEmpty(basePath) || isEmpty(moduleName)) {
            return null;
        }
        String returnPath = "";
        String targetPath = basePath + File.separator + moduleName + File.separator + "res" + File.separator;
        VirtualFile virtualFile = getVirtualFile(targetPath);
        if (virtualFile == null || !virtualFile.isValid() || !virtualFile.isDirectory()) {
            targetPath = basePath + File.separator + moduleName + File.separator + "src" + File.separator + "main" + File.separator + "res" + File.separator;
            virtualFile = getVirtualFile(targetPath);
            if (virtualFile != null && virtualFile.isValid() && virtualFile.isDirectory()) {
                returnPath = targetPath;
            }
        } else {
            returnPath = targetPath;
        }
        return returnPath;
    }

    private static String getAndroidManifestPath(String basePath, String moduleName) {
        if (isEmpty(basePath) || isEmpty(moduleName)) {
            return null;
        }
        String returnPath = "";
        String targetPath = basePath + File.separator + moduleName + File.separator + "AndroidManifest.xml";
        VirtualFile virtualFile = getVirtualFile(targetPath);
        if (virtualFile == null || !virtualFile.isValid() || !virtualFile.isDirectory()) {
            targetPath = basePath + File.separator + moduleName + File.separator + "src" + File.separator + "main" + File.separator + "AndroidManifest.xml";
            virtualFile = getVirtualFile(targetPath);
            if (virtualFile != null && virtualFile.isValid() && virtualFile.isDirectory()) {
                returnPath = targetPath;
            }
        } else {
            returnPath = targetPath;
        }
        return returnPath;
    }

    //获取包名字符串
    public static String getAppPackageName(Project project, String moduleName) {
        if (project == null || isEmpty(moduleName)) {
            return null;
        }
        PsiFile manifestFile = null;
        try {
            manifestFile = getPsiFile(getVirtualFile(getAndroidManifestPath(getBasePath(project), moduleName)), project);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (manifestFile == null) {
            return null;
        }
        XmlDocument xml = (XmlDocument) manifestFile.getFirstChild();
        if (xml == null) {
            return null;
        }
        XmlTag xmlTag = xml.getRootTag();
        if (xmlTag == null) {
            return null;
        }
        XmlAttribute xmlAttribute = xmlTag.getAttribute("package");
        if (xmlAttribute == null) {
            return null;
        }
        return xmlAttribute.getValue();
    }
}
