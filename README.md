# Image Coordinates Tool

Windows tool for opening PNG/JPG images and selecting rectangular areas. The side panel shows:

- cursor coordinates on the image;
- left top corner of the selected rectangle;
- right bottom corner of the selected rectangle;
- selected rectangle size.

After a rectangle is selected, drag its border or corner with the left mouse button to resize it.

## Run from Maven

```powershell
.\mvnw.cmd javafx:run
```

Open an image from inside the app with `File -> Open image...`.

## Open With on Windows

Build the Windows app image:

```powershell
.\scripts\package-windows.cmd
```

Then choose this executable in Windows `Open with`:

```text
target\windows\ImageCoordinatesTool\ImageCoordinatesTool.exe
```

The app accepts the image path as the first command-line argument, so Windows can pass a PNG/JPG file directly to it.
