package haven;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class MinimapIcons {
    public static List<String> res = new ArrayList<String>();
    public static List<String> ToggledIcons = new ArrayList<String>();

    public static void readnames() throws IOException {
        JarFile jarFile = new JarFile("custom-res.jar");
        final Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            final JarEntry entry = entries.nextElement();
            if (entry.getName().contains("gfx/minimap") && entry.getName().contains(".res")) {
                res.add(entry.getName());
                JarEntry fileEntry = jarFile.getJarEntry(entry.getName());
                InputStream input = jarFile.getInputStream(fileEntry);
            }
        }
    }

    public static void readToggles() throws IOException {
        try {
            Scanner scanner = new Scanner(new FileReader("ToggledIcons.Darki"));
            while (scanner.hasNext()) {
                ToggledIcons.add(scanner.next());
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            System.out.println("Couldn't find Icon File");
            e.printStackTrace();
        }
    }

    public static void writeToggle(String name) throws IOException {
        try {
            FileWriter fw = new FileWriter("ToggledIcons.Darki", true);
            BufferedWriter bw = new BufferedWriter(fw);
            if (!ToggledIcons.equals(name)) {
                bw.write(name);
                bw.newLine();
                ToggledIcons.add(name);
            }
            bw.close();
        } catch (FileNotFoundException e) {
            System.out.println("Couldn't find writable File");
            e.printStackTrace();
        }
    }

    public static void removeToggle(String name) throws IOException {
        try {
            File inputFile = new File("ToggledIcons.Darki");
            File tempFile = new File("myTempFile.Darki");

            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

            String lineToRemove = name;
            String currentLine;

            while((currentLine = reader.readLine()) != null) {
                String trimmedLine = currentLine.trim();
                if(trimmedLine.equals(lineToRemove)){
                    ToggledIcons.remove(ToggledIcons.indexOf(name));
                    continue;
                }
                writer.write(currentLine + System.getProperty("line.separator"));
            }
            writer.close();
            reader.close();
            inputFile.delete();
            tempFile.renameTo(inputFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static List<String> readspecific(String type) {
        List<String> speclist = new ArrayList<String>();
        for (String seperate : res) {
            if (seperate.contains(type)) {
                speclist.add(seperate.split("/")[4]);
            }
        }
        return (speclist);
    }

    public static void addSelection(String type, Window WindowName) {
        int x = 0, y = 0;
        for (String nameres : MinimapIcons.readspecific(type)) {
            String name = nameres.split("\\.")[0];
            if (y > 400) {
                y = 0;
                x += 80;
            }
            WindowName.add(new MinimapCheckBox(name) {
                public boolean mousedown(Coord c, int button) {
                    try {
                        if(!this.CheckBoxStatus()){
                            writeToggle(this.name);
                        }
                        if(this.CheckBoxStatus()){
                            removeToggle(this.name);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (button != 1) {
                        return (false);
                    }
                    set(!a);
                    return (true);
                }
            }, new Coord(x, y));
            y += 35;
        }
    }
}

