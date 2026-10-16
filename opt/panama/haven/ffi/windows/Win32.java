/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven.ffi.windows;

import haven.*;
import haven.ffi.*;
import java.lang.invoke.*;
import java.lang.foreign.*;
import java.util.*;
import java.util.function.*;
import java.nio.*;
import java.nio.charset.Charset;
import java.lang.foreign.MemoryLayout.PathElement;
import static haven.ffi.ABI.*;
import static haven.ffi.FUtils.*;
import static java.lang.foreign.ValueLayout.ADDRESS;

public abstract class Win32 {
    public static final Charset latin1 = Charset.forName("ISO-8859-1");

    public static final int S_OK = 0;

    public static final int ERROR_SUCCESS = 0;
    public static final int ERROR_FILE_NOT_FOUND = 2;
    public static final int ERROR_ACCESS_DENIED = 5;
    public static final int ERROR_MORE_DATA = 234;

    public static final int GMEM_FIXED    = 0x0000;
    public static final int GMEM_MOVEABLE = 0x0002;
    public static final int GMEM_ZEROINIT = 0x0040;

    public static final int WS_OVERLAPPED = 0x00000000;
    public static final int WS_MAXIMIZEBOX = 0x00010000;
    public static final int WS_MINIMIZEBOX = 0x00020000;
    public static final int WS_THICKFRAME = 0x00040000;
    public static final int WS_SYSMENU = 0x00080000;
    public static final int WS_BORDER = 0x00800000;
    public static final int WS_CAPTION = 0x00C00000;
    public static final int WS_POPUP = 0x80000000;
    public static final int WS_OVERLAPPEDWINDOW = WS_OVERLAPPED | WS_CAPTION | WS_SYSMENU | WS_THICKFRAME | WS_MINIMIZEBOX | WS_MAXIMIZEBOX;
    public static final int WS_POPUPWINDOW = WS_POPUP | WS_BORDER | WS_SYSMENU;
    public static final int WS_EX_APPWINDOW = 0x00040000;

    public static final int CW_USEDEFAULT = 0x80000000;

    public static final int GWL_WNDPROC = -4;
    public static final int GWL_HINSTANCE = -6;
    public static final int GWL_HWNDPARENT = -8;
    public static final int GWL_ID = -12;
    public static final int GWL_STYLE = -16;
    public static final int GWL_EXSTYLE = -20;
    public static final int GWL_USERDATA = -21;

    public static final int PM_NOREMOVE = 0x0000;
    public static final int PM_REMOVE = 0x0001;
    public static final int PM_NOYIELD = 0x0002;

    public static final int CS_OWNDC = 0x0020;

    public static final int SW_HIDE = 0;
    public static final int SW_NORMAL = 1;
    public static final int SW_SHOWMINIMIZED = 2;
    public static final int SW_MAXIMIZE = 3;
    public static final int SW_SHOWNOACTIVATE = 4;
    public static final int SW_SHOW = 5;
    public static final int SW_MINIMIZE = 6;
    public static final int SW_SHOWMINNOACTIVE = 7;
    public static final int SW_SHOWNA = 8;
    public static final int SW_RESTORE = 9;
    public static final int SW_SHOWDEFAULT = 10;
    public static final int SW_FORCEMINIMIZE = 11;

    public static final int SWP_NOSIZE         = 0x0001;
    public static final int SWP_NOMOVE         = 0x0002;
    public static final int SWP_NOZORDER       = 0x0004;
    public static final int SWP_NOREDRAW       = 0x0008;
    public static final int SWP_NOACTIVATE     = 0x0010;
    public static final int SWP_DRAWFRAME      = 0x0020;
    public static final int SWP_FRAMECHANGED   = 0x0020;
    public static final int SWP_SHOWWINDOW     = 0x0040;
    public static final int SWP_HIDEWINDOW     = 0x0080;
    public static final int SWP_NOCOPYBITS     = 0x0100;
    public static final int SWP_NOREPOSITION   = 0x0200;
    public static final int SWP_NOSENDCHANGING = 0x0400;
    public static final int SWP_DEFERERASE     = 0x2000;
    public static final int SWP_ASYNCWINDOWPOS = 0x4000;

    public static final int WM_SIZE                 = 0x0005;
    public static final int WM_SETFOCUS             = 0x0007;
    public static final int WM_KILLFOCUS            = 0x0008;
    public static final int WM_CLOSE                = 0x0010;
    public static final int WM_SHOWWINDOW           = 0x0018;
    public static final int WM_SETCURSOR            = 0x0020;
    public static final int WM_GETMINMAXINFO        = 0x0024;
    public static final int WM_SETICON              = 0x0080;
    public static final int WM_KEYDOWN              = 0x0100;
    public static final int WM_KEYUP                = 0x0101;
    public static final int WM_CHAR                 = 0x0102;
    public static final int WM_SYSKEYDOWN           = 0x0104;
    public static final int WM_SYSKEYUP             = 0x0105;
    public static final int WM_SYSCHAR              = 0x0106;
    public static final int WM_IME_STARTCOMPOSITION = 0x010d;
    public static final int WM_IME_ENDCOMPOSITION   = 0x010e;
    public static final int WM_IME_COMPOSITION      = 0x010f;
    public static final int WM_SYSCOMMAND           = 0x0112;
    public static final int WM_MOUSEMOVE            = 0x0200;
    public static final int WM_LBUTTONDOWN          = 0x0201;
    public static final int WM_LBUTTONUP            = 0x0202;
    public static final int WM_RBUTTONDOWN          = 0x0204;
    public static final int WM_RBUTTONUP            = 0x0205;
    public static final int WM_MBUTTONDOWN          = 0x0207;
    public static final int WM_MBUTTONUP            = 0x0208;
    public static final int WM_MOUSEWHEEL           = 0x020a;
    public static final int WM_XBUTTONDOWN          = 0x020b;
    public static final int WM_XBUTTONUP            = 0x020c;
    public static final int WM_HMOUSEWHEEL          = 0x020e;
    public static final int WM_IME_SETCONTEXT       = 0x0281;
    public static final int WM_IME_NOTIFY           = 0x0282;
    public static final int WM_IME_CONTROL          = 0x0283;
    public static final int WM_IME_COMPOSITIONFULL  = 0x0284;
    public static final int WM_IME_SELECT           = 0x0285;
    public static final int WM_IME_CHAR             = 0x0286;
    public static final int WM_IME_REQUEST          = 0x0288;
    public static final int WM_IME_KEYDOWN          = 0x0290;
    public static final int WM_IME_KEYUP            = 0x0291;
    public static final int WM_DESTROYCLIPBOARD     = 0x0307;
    public static final int WM_USER                 = 0x0400;
    public static final int WM_APP                  = 0x8000;

    public static final int SC_KEYMENU = 0xf100;
    public static final int ICON_SMALL = 0;
    public static final int ICON_BIG = 1;
    public static final int HTCLIENT = 1;

    public static final int IDC_ARROW = 32512;
    public static final int IDC_IBEAM = 32513;
    public static final int IDC_WAIT = 32514;
    public static final int IDC_CROSS = 32515;
    public static final int IDC_UPARROW = 32516;
    public static final int IDC_SIZENWSE = 32642;
    public static final int IDC_SIZENESW = 32643;
    public static final int IDC_SIZEWE = 32644;
    public static final int IDC_SIZENS = 32645;
    public static final int IDC_SIZEALL = 32646;
    public static final int IDC_NO = 32648;
    public static final int IDC_HAND = 32649;
    public static final int IDC_APPSTARTING = 32650;
    public static final int IDC_HELP = 32651;
    public static final int IDC_PIN = 32671;
    public static final int IDC_PERSON = 32672;

    public static final int SIZE_RESTORED = 0;
    public static final int SIZE_MINIMIZED = 1;
    public static final int SIZE_MAXIMIZED = 2;
    public static final int SIZE_MAXSHOW = 3;
    public static final int SIZE_MAXHIDE = 4;

    public static final int MAPVK_VK_TO_VSC    = 0;
    public static final int MAPVK_VSC_TO_VK    = 1;
    public static final int MAPVK_VK_TO_CHAR   = 2;
    public static final int MAPVK_VSC_TO_VK_EX = 3;
    public static final int MAPVK_VK_TO_VSC_EX = 4;

    public static final int MK_LBUTTON  = 0x0001;
    public static final int MK_RBUTTON  = 0x0002;
    public static final int MK_SHIFT    = 0x0004;
    public static final int MK_CONTROL  = 0x0008;
    public static final int MK_MBUTTON  = 0x0010;
    public static final int MK_XBUTTON1 = 0x0020;
    public static final int MK_XBUTTON2 = 0x0040;

    public static final int VK_PROCESSKEY = 0xe5;

    public static final int RRF_RT_ANY = 0xffff;
    public static final int REG_NONE = 0;
    public static final int REG_SZ = 1;
    public static final int REG_EXPAND_SZ = 2;
    public static final int REG_BINARY = 3;
    public static final int REG_DWORD = 4;
    public static final int REG_DWORD_LITTLE_ENDIAN = 4;
    public static final int REG_DWORD_BIG_ENDIAN = 5;
    public static final int REG_LINK = 6;
    public static final int REG_MULTI_SZ = 7;
    public static final int REG_RESOURCE_LIST = 8;
    public static final int REG_FULL_RESOURCE_DESCRIPTOR = 9;
    public static final int REG_RESOURCE_REQUIREMENTS_LIST = 10;
    public static final int REG_QWORD = 11;
    public static final int REG_QWORD_LITTLE_ENDIAN = 11;

    public static final int PFD_DOUBLEBUFFER   = 0x00000001;
    public static final int PFD_DRAW_TO_WINDOW = 0x00000004;
    public static final int PFD_SUPPORT_OPENGL = 0x00000020;
    public static final int PFD_TYPE_RGBA = 0;

    public static final int MONITOR_DEFAULTTONULL = 0;
    public static final int MONITOR_DEFAULTTOPRIMARY = 1;
    public static final int MONITOR_DEFAULTTONEAREST = 2;

    public static final int HORZSIZE   = 4;
    public static final int VERTSIZE   = 6;
    public static final int HORZRES    = 8;
    public static final int VERTRES    = 10;
    public static final int LOGPIXELSX = 88;
    public static final int LOGPIXELSY = 90;

    public static final int SM_CXCURSOR = 13;
    public static final int SM_CYCURSOR = 14;

    public static final int BI_RGB       =  0;
    public static final int BI_RLE8      =  1;
    public static final int BI_RLE4      =  2;
    public static final int BI_BITFIELDS =  3;
    public static final int BI_JPEG      =  4;
    public static final int BI_PNG       =  5;
    public static final int BI_CMYK      = 11;
    public static final int BI_CMYKRLE8  = 12;
    public static final int BI_CMYKRLE4  = 13;
    public static final int DIB_RGB_COLORS  = 0;
    public static final int DIB_PAL_COLORS  = 1;
    public static final int DIB_PAL_INDICES = 2;

    public static final int CF_TEXT = 1;
    public static final int CF_BITMAP = 2;
    public static final int CF_TIFF = 6;
    public static final int CF_DIB = 8;
    public static final int CF_UNICODETEXT = 13;
    public static final int CF_HDROP = 15;
    public static final int CF_DIBV5 = 17;

    public static final Handle HKEY_CLASSES_ROOT = Handle.of(0x80000000);
    public static final Handle HKEY_CURRENT_USER = Handle.of(0x80000001);
    public static final Handle HKEY_LOCAL_MACHINE = Handle.of(0x80000002);
    public static final Handle HKEY_USERS = Handle.of(3);

    public static class Handle {
	public static final Handle nil = new Handle(MemorySegment.NULL);
	final MemorySegment bits;

	private Handle(MemorySegment bits) {this.bits = bits;}
	static Handle of(MemorySegment bits) {return(nullp(bits) ? nil : new Handle(bits));}
	static Handle of(long bits) {return((bits == 0) ? nil : new Handle(MemorySegment.ofAddress(bits)));}

	public int hashCode() {return(Long.hashCode(bits.address()));}
	public boolean equals(Object x) {return((x instanceof Handle) && (((Handle)x).bits.equals(this.bits)));}
	public String toString() {
	    return("$" + Long.toUnsignedString(bits.address(), 16));
	}
    }

    public static interface WndProc {
	public long handle(Handle hwnd, int umsg, long wparam, long lparam);
    }

    public static abstract class WndClassEx extends StructInstance {
	protected WndClassEx(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}

	public abstract WndClassEx style(int value);
	public abstract WndClassEx lpfnWndProc(WndProc value);
	public abstract WndClassEx hInstance(Handle value);
	public abstract WndClassEx lpszClassName(String value);
    }
    public abstract WndClassEx WndClassEx();

    public static abstract class POINT extends StructInstance {
	protected POINT(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}

	public abstract POINT x(int value);
	public abstract int x();
	public abstract POINT y(int value);
	public abstract int y();

	public Coord c() {return(Coord.of(x(), y()));}
	public POINT c(Coord c) {x(c.x); y(c.y); return(this);}
    }
    public abstract MemoryLayout C_POINT();
    public abstract POINT POINT(MemorySegment mem);
    public abstract POINT POINT(SegmentAllocator alloc);
    public abstract POINT POINT();
    public POINT POINT(Coord c) {return(POINT().c(c));}

    public static abstract class MSG extends StructInstance {
	protected MSG(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}
	public abstract Handle hwnd();
	public abstract int message();
	public abstract long wparam();
	public abstract long lparam();
    }
    public abstract MSG MSG();

    public static abstract class MINMAXINFO extends StructInstance {
	protected MINMAXINFO(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}

	public abstract Coord ptMaxSize();
	public abstract MINMAXINFO ptMaxSize(Coord v);
	public abstract Coord ptMaxPosition();
	public abstract MINMAXINFO ptMaxPosition(Coord v);
	public abstract Coord ptMinTrackSize();
	public abstract MINMAXINFO ptMinTrackSize(Coord v);
	public abstract Coord ptMaxTrackSize();
	public abstract MINMAXINFO ptMaxTrackSize(Coord v);
    }
    public abstract MINMAXINFO MINMAXINFO(long lparam);

    public static class KeyboardState {
	final MemorySegment keys;

	public KeyboardState() {
	    keys = Arena.ofAuto().allocate(256);
	}

	public int get(int vk) {
	    return(keys.get(ValueLayout.JAVA_BYTE, vk) & 0xff);
	}

	public KeyboardState set(int vk, int val) {
	    keys.set(ValueLayout.JAVA_BYTE, vk, (byte)val);
	    return(this);
	}
    }

    public static abstract class MONITORINFO extends StructInstance {
	protected MONITORINFO(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}

	public abstract Area rcMonitor();
	public abstract Area rcWork();
	public abstract int dwFlags();
    }
    public abstract MONITORINFO MONITORINFO();

    public static abstract class BITMAPV5HEADER extends StructInstance {
	protected BITMAPV5HEADER(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}

	public abstract long sizeof();

	public abstract int bV5Size();
	public abstract int bV5Width();
	public abstract int bV5Height();
	public abstract int bV5BitCount();
	public abstract int bV5Compression();
	public abstract int bV5SizeImage();
	public abstract int bV5ClrUsed();
	public abstract int bV5RedMask();
	public abstract int bV5GreenMask();
	public abstract int bV5BlueMask();
	public abstract int bV5AlphaMask();

	public abstract BITMAPV5HEADER bV5Size(int v);
	public abstract BITMAPV5HEADER bV5Width(int v);
	public abstract BITMAPV5HEADER bV5Height(int v);
	public abstract BITMAPV5HEADER bV5Planes(int v);
	public abstract BITMAPV5HEADER bV5BitCount(int v);
	public abstract BITMAPV5HEADER bV5Compression(int v);
	public abstract BITMAPV5HEADER bV5SizeImage(int v);
	public abstract BITMAPV5HEADER bV5ClrUsed(int v);
	public abstract BITMAPV5HEADER bV5RedMask(int v);
	public abstract BITMAPV5HEADER bV5GreenMask(int v);
	public abstract BITMAPV5HEADER bV5BlueMask(int v);
	public abstract BITMAPV5HEADER bV5AlphaMask(int v);
    }
    public abstract int sizeof_BITMAPV5HEADER();
    public abstract BITMAPV5HEADER BITMAPV5HEADER();
    public abstract BITMAPV5HEADER BITMAPV5HEADER(MemorySegment mem);

    public static abstract class DROPFILES extends StructInstance {
	protected DROPFILES(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}

	public abstract long sizeof();

	public abstract int pFiles();
	public abstract Coord pt();
	public abstract boolean fNC();
	public abstract boolean fWide();

	public abstract DROPFILES pFiles(int v);
	public abstract DROPFILES pt(Coord v);
	public abstract DROPFILES fNC(boolean v);
	public abstract DROPFILES fWide(boolean v);
    }
    public abstract int sizeof_DROPFILES();
    public abstract DROPFILES DROPFILES(int extra);
    public abstract DROPFILES DROPFILES(MemorySegment mem);

    public static abstract class ICONINFO extends StructInstance {
	protected ICONINFO(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}

	public abstract ICONINFO fIcon(int v);
	public abstract ICONINFO xHotspot(int v);
	public abstract ICONINFO yHotspot(int v);
	public abstract ICONINFO hbmMask(Handle v);
	public abstract ICONINFO hbmColor(Handle v);
    }
    public abstract ICONINFO ICONINFO();

    public static abstract class SHELLEXECUTEINFO extends StructInstance {
	protected SHELLEXECUTEINFO(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}

	public abstract SHELLEXECUTEINFO lpVerb(String value);
	public abstract SHELLEXECUTEINFO lpFile(String value);
	public abstract SHELLEXECUTEINFO lpParameters(String value);
	public abstract SHELLEXECUTEINFO lpDirectory(String value);
	public abstract SHELLEXECUTEINFO nShow(int value);
    }
    public abstract SHELLEXECUTEINFO SHELLEXECUTEINFO();

    public static abstract class PIXELFORMATDESCRIPTOR extends StructInstance {
	protected PIXELFORMATDESCRIPTOR(MemorySegment mem) {
	    super(mem);
	}

	MemorySegment mem() {return(mem);}

	public abstract PIXELFORMATDESCRIPTOR nVersion(int value);
	public abstract PIXELFORMATDESCRIPTOR dwFlags(int value);
	public abstract PIXELFORMATDESCRIPTOR iPixelType(int value);
	public abstract PIXELFORMATDESCRIPTOR cColorBits(int value);
	public abstract PIXELFORMATDESCRIPTOR cAlphaBits(int value);
	public abstract PIXELFORMATDESCRIPTOR cAccumBits(int value);
	public abstract PIXELFORMATDESCRIPTOR cDepthBits(int value);
	public abstract PIXELFORMATDESCRIPTOR cStencilBits(int value);
	public abstract PIXELFORMATDESCRIPTOR cAuxBuffers(int value);
	public abstract PIXELFORMATDESCRIPTOR iLayerType(int value);
    }
    public abstract PIXELFORMATDESCRIPTOR PIXELFORMATDESCRIPTOR();

    public abstract Charset C_WCHARSET();

    public abstract int GetLastError();
    public abstract int GetCurrentThreadId();
    public abstract Handle GetModuleHandle(String lpModuleName);
    public abstract int GetACP();
    public abstract Handle GlobalAlloc(int flags, int bytes);
    public abstract void GlobalFree(Handle mem);
    public abstract long GlobalSize(Handle mem);
    public abstract MemorySegment GlobalLock(Handle mem);
    public abstract boolean GlobalUnlock(Handle mem);
    public abstract int RegisterClassEx(WndClassEx cls);
    public abstract void UnregisterClass(String lpClassName, Handle hInstance);
    public abstract Handle CreateWindowEx(int dwExStyle, String lpClassName, String lpWindowName, int dwStyle, Coord pos, Coord size, Handle hWndParent, Handle hMenu, Handle hInstance);
    public abstract int DestroyWindow(Handle hWnd);
    public abstract boolean GetMessage(MSG lpMsg, Handle hWnd, int wMsgFilterMin, int wMsgFilterMax);
    public abstract boolean PeekMessage(Win32.MSG lpMsg, Handle hWnd, int wMsgFilterMin, int wMsgFilterMax, int wRemoveMsg);
    public abstract boolean TranslateMessage(Win32.MSG lpMsg);
    public abstract long DispatchMessage(Win32.MSG lpMsg);
    public abstract void PostMessage(Handle hWnd, int Msg, long wParam, long lParam);
    public abstract int SendMessage(Handle hWnd, int Msg, long wParam, long lParam);
    public abstract void PostThreadMessage(int idThread, int Msg, long wParam, long lParam);
    public abstract long DefWindowProc(Handle hWnd, int Msg, long wParam, long lParam);
    public abstract boolean ShowWindow(Handle hWnd, int nCmdShow);
    public abstract long GetWindowLongPtr(Handle hWnd, int nIndex);
    public abstract long SetWindowLongPtr(Handle hWnd, int nIndex, long dwNewLong);
    public abstract Area GetWindowRect(Handle hWnd);
    public abstract void SetWindowPos(Handle hWnd, Handle hWndInsertAfter, Coord pos, Coord size, int uFlags);
    public abstract Area AdjustWindowRectEx(Area rect, int dwStyle, boolean bMenu, int dwExStyle);
    public abstract void SetWindowText(Handle hWnd, String lpString);
    public abstract Coord ScreenToClient(Handle hWnd, Coord point);
    public abstract Coord ClientToScreen(Handle hWnd, Coord point);
    public abstract Handle WindowFromPoint(Coord point);
    public abstract List<Handle> EnumDisplayMonitors();
    public abstract Handle MonitorFromWindow(Handle hWnd, int dwFlags);
    public abstract void GetMonitorInfo(Handle hMonitor, MONITORINFO lpmi);
    public abstract Handle GetDC(Handle hWnd);
    public abstract boolean ReleaseDC(Handle hWnd, Handle hDC);
    public abstract int GetDeviceCaps(Handle hdc, int index);
    public abstract int GetSystemMetrics(int nIndex);
    public abstract int GetKeyState(int nVirtKey);
    public abstract void GetKeyboardState(KeyboardState buf);
    public abstract Handle GetKeyboardLayout(int idThread);
    public abstract Handle[] GetKeyboardLayoutList();
    public abstract int MapVirtualKeyEx(int uCode, int uMapType, Handle dwhkl);
    public abstract int VkKeyScanEx(int ch, Handle dwhkl);
    public abstract char[] ToUnicodeEx(int wVirtKey, int wScanCode, KeyboardState lpKeyState, int wFlags, Handle dwhkl);
    public abstract String GetKeyNameText(long lParam);
    public abstract Handle CreateDIBSection(Handle hDC, BITMAPV5HEADER pbmi, int usage, ByteBuffer[] ppvBits, Handle hSection, int offset);
    public abstract Handle CreateBitmap(int nWidth, int nHeight, int nPlanes, int nBitCount, ByteBuffer lpBits);
    public abstract Handle CreateIconIndirect(ICONINFO piconinfo);
    public abstract Handle SetCursor(Handle hCursor);
    public abstract Handle LoadCursor(Handle hInstance, int cursor);
    public abstract void DestroyIcon(Handle ho);
    public abstract int RegisterClipboardFormat(String format);
    public abstract String GetClipboardFormatName(int format);
    public abstract void OpenClipboard(Handle newowner);
    public abstract void CloseClipboard();
    public abstract boolean IsClipboardFormatAvailable(int format);
    public abstract Handle GetClipboardData(int format);
    public abstract void EmptyClipboard();
    public abstract void SetClipboardData(int format, Handle mem);
    public abstract Object RegGetValue(Handle hKey, String lpSubKey, String lpValue, int dwFlags);
    public abstract void DeleteObject(Handle ho);
    public abstract void ShellExecuteEx(Win32.SHELLEXECUTEINFO pExecInfo);
    public abstract int ChoosePixelFormat(Handle hDC, PIXELFORMATDESCRIPTOR ppfd);
    public abstract void SetPixelFormat(Handle hDC, int format, Win32.PIXELFORMATDESCRIPTOR ppfd);
    public abstract int DescribePixelFormat(Handle hDC, int iPixelFormat, Win32.PIXELFORMATDESCRIPTOR ppfd);
    public abstract void SwapBuffers(Handle hDC);

    public int SendMessage(Handle hWnd, int Msg, long wParam, Handle lParam) {return(SendMessage(hWnd, Msg, wParam, nhandle(lParam).address()));}

    public RuntimeException lasterror() {
	return(new StdError(GetLastError()));
    }

    static MemorySegment nhandle(Handle h) {
	if(h == null)
	    return(MemorySegment.NULL);
	return(h.bits);
    }

    private static final Map<Integer, Charset> codepages = new HashMap<>();
    public Charset codepage(int id, Charset def) {
	switch(id) {
	case 65001: return(Utils.utf8);
	case 25891: return(latin1);
	}
	Charset ret;
	synchronized(codepages) {
	    if(!codepages.containsKey(id)) {
		String name;
		switch(id) {
		case 932: name = "Shift_JIS"; break;
		case 936: name = "GBK";       break;
		case 949: name = "EUC-KR";    break;
		case 950: name = "Big5";      break;
		default:  name = "cp" + id;   break;
		}
		try {
		    ret = Charset.forName(name);
		} catch(IllegalArgumentException e) {
		    ret = null;
		}
		codepages.put(id, ret);
	    }
	    ret = codepages.get(id);
	}
	return((ret == null) ? def : ret);
    }

    static class Win64Unicode extends Win32 {
	static final Charset C_WCHARSET = Charset.forName(ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? "UTF-16LE" : "UTF-16BE");
	static final MemoryLayout WCHAR = ValueLayout.JAVA_SHORT;
	static final MemoryLayout BOOL = C_INT;
	static final MemoryLayout BYTE = C_CHAR;
	static final MemoryLayout SHORT = C_SHORT;
	static final MemoryLayout UINT = C_INT;
	static final MemoryLayout LONG = C_LONG;
	static final MemoryLayout ULONG = C_LONG;
	static final MemoryLayout WORD = C_SHORT;
	static final MemoryLayout DWORD = C_LONG;
	static final MemoryLayout PVOID = ADDRESS;
	static final MemoryLayout LPVOID = ADDRESS;
	static final MemoryLayout LONG_PTR = PTRINT_T;
	static final MemoryLayout INT_PTR = PTRINT_T;
	static final MemoryLayout ULONG_PTR = PTRINT_T;
	static final MemoryLayout UINT_PTR = PTRINT_T;
	static final MemoryLayout HANDLE = PVOID;
	static final MemoryLayout HBITMAP = HANDLE;
	static final MemoryLayout HBRUSH = HANDLE;
	static final MemoryLayout HCURSOR = HANDLE;
	static final MemoryLayout HDC = HANDLE;
	static final MemoryLayout HGDIOBJ = HANDLE;
	static final MemoryLayout HGLOBAL = HANDLE;
	static final MemoryLayout HICON = HANDLE;
	static final MemoryLayout HINSTANCE = HANDLE;
	static final MemoryLayout HKEY = HANDLE;
	static final MemoryLayout HKL = HANDLE;
	static final MemoryLayout HMENU = HANDLE;
	static final MemoryLayout HMONITOR = HANDLE;
	static final MemoryLayout HRESULT = LONG;
	static final MemoryLayout HWND = HANDLE;
	static final MemoryLayout ATOM = WORD;
	static final MemoryLayout WNDPROC = HANDLE;
	static final MemoryLayout LPWSTR = ADDRESS;
	static final MemoryLayout LPCWSTR = ADDRESS;
	static final MemoryLayout LRESULT = LONG_PTR;
	static final MemoryLayout LSTATUS = LONG;
	static final MemoryLayout WPARAM = UINT_PTR;
	static final MemoryLayout LPARAM = LONG_PTR;
	static final MemoryLayout CLIPFORMAT = WORD;
	private final SymbolLookup kernel32 = loadlib("KERNEL32.DLL", Arena.global());
	private final SymbolLookup user32 = loadlib("USER32.DLL", Arena.global());
	private final SymbolLookup gdi32 = loadlib("GDI32.DLL", Arena.global());
	private final SymbolLookup shell32 = loadlib("SHELL32.DLL", Arena.global());
	private final SymbolLookup advapi32 = loadlib("ADVAPI32.DLL", Arena.global());

	public Charset C_WCHARSET() {return(C_WCHARSET);}

	static MemorySegment wstr(Arena st, String str) {
	    if(str == null)
		return(MemorySegment.NULL);
	    return(st.allocateFrom(str, C_WCHARSET));
	}

	private final MethodHandle GetCurrentThreadId = ld.downcallHandle(kernel32.find("GetCurrentThreadId").get(), FunctionDescriptor.of(DWORD));
	public int GetCurrentThreadId() {
	    try {
		return((int)GetCurrentThreadId.invoke());
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private final MethodHandle GetLastError = ld.downcallHandle(kernel32.find("GetLastError").get(), FunctionDescriptor.of(DWORD));
	public int GetLastError() {
	    try {
		return((int)GetLastError.invoke());
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private final MethodHandle GetModuleHandleW = ld.downcallHandle(kernel32.find("GetModuleHandleW").get(), FunctionDescriptor.of(HINSTANCE, LPCWSTR));
	public Handle GetModuleHandle(String lpModuleName) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment rv;
		try {
		    rv = (MemorySegment)GetModuleHandleW.invoke(wstr(st, lpModuleName));
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(nullp(rv))
		    throw(lasterror());
		return(Handle.of(rv));
	    }
	}

	private final MethodHandle GetACP = ld.downcallHandle(kernel32.find("GetACP").get(), FunctionDescriptor.of(UINT));
	public int GetACP() {
	    try {
		return((int)GetACP.invoke());
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private final MethodHandle GlobalAlloc = ld.downcallHandle(kernel32.find("GlobalAlloc").get(), FunctionDescriptor.of(HGLOBAL, UINT, SIZE_T));
	public Handle GlobalAlloc(int flags, int bytes) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)GlobalAlloc.invoke(flags, bytes);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(nullp(rv))
		throw(lasterror());
	    return(Handle.of(rv));
	}

	private final MethodHandle GlobalFree = ld.downcallHandle(kernel32.find("GlobalFree").get(), FunctionDescriptor.of(HGLOBAL, HGLOBAL));
	public void GlobalFree(Handle mem) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)GlobalFree.invoke(mem.bits);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(!nullp(rv))
		throw(lasterror());
	}

	private final MethodHandle GlobalSize = ld.downcallHandle(kernel32.find("GlobalSize").get(), FunctionDescriptor.of(SIZE_T, HGLOBAL));
	public long GlobalSize(Handle mem) {
	    try {
		return((long)GlobalSize.invoke(mem.bits));
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private final MethodHandle GlobalLock = ld.downcallHandle(kernel32.find("GlobalLock").get(), FunctionDescriptor.of(ADDRESS, HGLOBAL));
	public MemorySegment GlobalLock(Handle mem) {
	    long size = GlobalSize(mem);
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)GlobalLock.invoke(mem.bits);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(nullp(rv))
		throw(lasterror());
	    return(rv.reinterpret(size));
	}

	private final MethodHandle GlobalUnlock = ld.downcallHandle(kernel32.find("GlobalUnlock").get(), FunctionDescriptor.of(BOOL, HGLOBAL));
	public boolean GlobalUnlock(Handle mem) {
	    int rv;
	    try {
		rv = (int)GlobalUnlock.invoke(mem.bits);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv != 0)
		return(true);
	    int err = GetLastError();
	    if(err != ERROR_SUCCESS)
		throw(new StdError(err));
	    return(false);
	}

	static final StructLayout _WndClassExW = struct(new MemoryLayout[] {
		UINT.withName("cbSize"),
		UINT.withName("style"),
		WNDPROC.withName("lpfnWndProc"),
		C_INT.withName("cbClsExtra"),
		C_INT.withName("cbWndExtra"),
		HINSTANCE.withName("hInstance"),
		HICON.withName("hIcon"),
		HCURSOR.withName("hCursor"),
		HBRUSH.withName("hbrBackground"),
		LPCWSTR.withName("lpszMenuName"),
		LPCWSTR.withName("lpszClassName"),
		HICON.withName("hIconSm"),
	    });
	public static class WndClassExW extends Win32.WndClassEx {
	    private final Arena alloc;

	    private WndClassExW(Arena alloc) {
		super(alloc.allocate(_WndClassExW.byteSize()));
		this.alloc = alloc;
		cbSize.set(mem, 0, (int)_WndClassExW.byteSize());
	    }
	    protected WndClassExW() {
		this(Arena.ofAuto());
	    }

	    protected StructLayout $layout() {return(_WndClassExW);}

	    static long handle(WndProc wndproc, MemorySegment hwnd, int umsg, long wparam, long lparam) {
		try {
		    return(wndproc.handle(Handle.of(hwnd), umsg, wparam, lparam));
		} catch(Throwable t) {
		    Thread.UncaughtExceptionHandler h = Thread.currentThread().getUncaughtExceptionHandler();
		    if(h == null)
			new Warning(t, "Uncaught exception in wndproc").issue();
		    else
			h.uncaughtException(Thread.currentThread(), t);
		    return(0);
		}
	    }
	    static final MethodHandle handle = slookup(MethodHandles.lookup(), WndClassExW.class, "handle", Long.TYPE,
						       WndProc.class, MemorySegment.class, Integer.TYPE, Long.TYPE, Long.TYPE);

	    private static final VarHandle cbSize = _WndClassExW.varHandle(PathElement.groupElement("cbSize"));
	    private static final VarHandle style = _WndClassExW.varHandle(PathElement.groupElement("style"));
	    public WndClassExW style(int value) {style.set(mem, 0, value); return(this);}
	    private static final VarHandle hInstance = _WndClassExW.varHandle(PathElement.groupElement("hInstance"));
	    public WndClassExW hInstance(Handle value) {hInstance.set(mem, 0, nhandle(value)); return(this);}
	    private static final VarHandle lpszClassName = _WndClassExW.varHandle(PathElement.groupElement("lpszClassName"));
	    public WndClassExW lpszClassName(String value) {
		lpszClassName.set(mem, 0, wstr(alloc, value));
		return(this);
	    }

	    private static final VarHandle lpfnWndProc = _WndClassExW.varHandle(PathElement.groupElement("lpfnWndProc"));
	    private MemorySegment handlerp;
	    public WndClassExW lpfnWndProc(WndProc value) {
		MethodHandle handler = MethodHandles.insertArguments(handle, 0, value);
		handlerp = ld.upcallStub(handler, FunctionDescriptor.of(LRESULT, ADDRESS, UINT, WPARAM, LPARAM), alloc);
		lpfnWndProc.set(mem, 0, handlerp);
		return(this);
	    }
	}

	public WndClassExW WndClassEx() {
	    return(new WndClassExW());
	}

	private final MethodHandle RegisterClassExW = ld.downcallHandle(user32.find("RegisterClassExW").get(), FunctionDescriptor.of(ATOM, ADDRESS));
	public int RegisterClassEx(WndClassEx cls) {
	    try(Arena st = Arena.ofConfined()) {
		int rv;
		try {
		    rv = (short)RegisterClassExW.invoke(cls.mem()) & 0xffff;
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(rv == 0)
		    throw(lasterror());
		return(rv);
	    }
	}

	private final MethodHandle UnregisterClassW = ld.downcallHandle(user32.find("UnregisterClassW").get(), FunctionDescriptor.of(BOOL, LPCWSTR, HINSTANCE));
	public void UnregisterClass(String lpClassName, Handle hInstance) {
	    try(Arena st = Arena.ofConfined()) {
		int rv;
		try {
		    rv = (int)UnregisterClassW.invoke(wstr(st, lpClassName), hInstance.bits);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(rv == 0)
		    throw(lasterror());
	    }
	}

	private final MethodHandle CreateWindowExW = ld.downcallHandle(user32.find("CreateWindowExW").get(), FunctionDescriptor.of(HWND, DWORD, LPCWSTR, LPCWSTR, DWORD, C_INT, C_INT, C_INT, C_INT, HWND, HMENU, HINSTANCE, LPVOID));
	public Handle CreateWindowEx(int dwExStyle, String lpClassName, String lpWindowName, int dwStyle, Coord pos, Coord size, Handle hWndParent, Handle hMenu, Handle hInstance) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment rv;
		try {
		    rv = (MemorySegment)CreateWindowExW.invoke(dwExStyle, wstr(st, lpClassName), wstr(st, lpWindowName), dwStyle,
							       (pos == null) ? CW_USEDEFAULT : pos.x, (pos == null) ? CW_USEDEFAULT : pos.y,
							       (size == null) ? CW_USEDEFAULT : size.x, (size == null) ? CW_USEDEFAULT : size.y,
							       nhandle(hWndParent), nhandle(hMenu), nhandle(hInstance), MemorySegment.NULL);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(nullp(rv))
		    throw(lasterror());
		return(Handle.of(rv));
	    }
	}

	private final MethodHandle DestroyWindow = ld.downcallHandle(user32.find("DestroyWindow").get(), FunctionDescriptor.of(BOOL, HWND));
	public int DestroyWindow(Handle hWnd) {
	    int rv;
	    try {
		rv = (int)DestroyWindow.invoke(hWnd.bits);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv == 0)
		throw(lasterror());
	    return(rv);
	}

	static final StructLayout _POINT = struct(new MemoryLayout[] {
		LONG.withName("x"),
		LONG.withName("y"),
	    });
	public static class POINT extends Win32.POINT {
	    private POINT(MemorySegment mem) {
		super(mem);
	    }
	    private POINT(SegmentAllocator alloc) {
		this(alloc.allocate(_POINT));
	    }
	    protected POINT() {
		this(Arena.ofAuto());
	    }

	    protected StructLayout $layout() {return(_POINT);}

	    private static final VarHandle x = _POINT.varHandle(PathElement.groupElement("x"));
	    public POINT x(int value) {x.set(mem, 0, value); return(this);}
	    public int x() {return((int)x.get(mem, 0));}
	    private static final VarHandle y = _POINT.varHandle(PathElement.groupElement("y"));
	    public POINT y(int value) {y.set(mem, 0, value); return(this);}
	    public int y() {return((int)y.get(mem, 0));}
	}
	public MemoryLayout C_POINT() {return(_POINT);}
	public POINT POINT(MemorySegment mem) {
	    return(new POINT(mem));
	}
	public POINT POINT(SegmentAllocator alloc) {
	    return(new POINT(alloc));
	}
	public POINT POINT() {
	    return(new POINT());
	}

	static final StructLayout _RECT = struct(new MemoryLayout[] {
		LONG.withName("left"),
		LONG.withName("top"),
		LONG.withName("right"),
		LONG.withName("bottom"),
	    });

	static final StructLayout _MSG = struct(new MemoryLayout[] {
		HWND.withName("hwnd"),
		UINT.withName("message"),
		WPARAM.withName("wparam"),
		LPARAM.withName("lparam"),
		DWORD.withName("time"),
		_POINT.withName("pt"),
		DWORD.withName("lPrivate"),
	    });
	public static class MSG extends Win32.MSG {
	    private MSG(Arena alloc) {
		super(alloc.allocate(_MSG.byteSize()));
	    }
	    protected MSG() {
		this(Arena.ofAuto());
	    }

	    protected StructLayout $layout() {return(_MSG);}

	    private static final VarHandle hwnd = _MSG.varHandle(PathElement.groupElement("hwnd"));
	    public Handle hwnd() {return(Handle.of((MemorySegment)hwnd.get(mem, 0)));}
	    private static final VarHandle message = _MSG.varHandle(PathElement.groupElement("message"));
	    public int message() {return((int)message.get(mem, 0));}
	    private static final VarHandle wparam = _MSG.varHandle(PathElement.groupElement("wparam"));
	    public long wparam() {return((long)wparam.get(mem, 0));}
	    private static final VarHandle lparam = _MSG.varHandle(PathElement.groupElement("lparam"));
	    public long lparam() {return((long)lparam.get(mem, 0));}
	}

	public MSG MSG() {
	    return(new MSG());
	}

	static final StructLayout _MINMAXINFO = struct(new MemoryLayout[] {
		_POINT.withName("ptReserved"),
		_POINT.withName("ptMaxSize"),
		_POINT.withName("ptMaxPosition"),
		_POINT.withName("ptMinTrackSize"),
		_POINT.withName("ptMaxTrackSize"),
	    });
	public static class MINMAXINFO extends Win32.MINMAXINFO {
	    private MINMAXINFO(MemorySegment mem) {
		super(mem);
	    }

	    protected StructLayout $layout() {return(_MINMAXINFO);}

	    private static final VarHandle ptMaxSize_x = _MINMAXINFO.varHandle(PathElement.groupElement("ptMaxSize"), PathElement.groupElement("x"));
	    private static final VarHandle ptMaxSize_y = _MINMAXINFO.varHandle(PathElement.groupElement("ptMaxSize"), PathElement.groupElement("y"));
	    public Coord ptMaxSize() {return(Coord.of((int)ptMaxSize_x.get(mem, 0), (int)ptMaxSize_y.get(mem, 0)));}
	    public MINMAXINFO ptMaxSize(Coord v) {ptMaxSize_x.set(mem, 0, v.x); ptMaxSize_y.set(mem, 0, v.y); return(this);}
	    private static final VarHandle ptMaxPosition_x = _MINMAXINFO.varHandle(PathElement.groupElement("ptMaxPosition"), PathElement.groupElement("x"));
	    private static final VarHandle ptMaxPosition_y = _MINMAXINFO.varHandle(PathElement.groupElement("ptMaxPosition"), PathElement.groupElement("y"));
	    public Coord ptMaxPosition() {return(Coord.of((int)ptMaxPosition_x.get(mem, 0), (int)ptMaxPosition_y.get(mem, 0)));}
	    public MINMAXINFO ptMaxPosition(Coord v) {ptMaxPosition_x.set(mem, 0, v.x); ptMaxPosition_y.set(mem, 0, v.y); return(this);}
	    private static final VarHandle ptMinTrackSize_x = _MINMAXINFO.varHandle(PathElement.groupElement("ptMinTrackSize"), PathElement.groupElement("x"));
	    private static final VarHandle ptMinTrackSize_y = _MINMAXINFO.varHandle(PathElement.groupElement("ptMinTrackSize"), PathElement.groupElement("y"));
	    public Coord ptMinTrackSize() {return(Coord.of((int)ptMinTrackSize_x.get(mem, 0), (int)ptMinTrackSize_y.get(mem, 0)));}
	    public MINMAXINFO ptMinTrackSize(Coord v) {ptMinTrackSize_x.set(mem, 0, v.x); ptMinTrackSize_y.set(mem, 0, v.y); return(this);}
	    private static final VarHandle ptMaxTrackSize_x = _MINMAXINFO.varHandle(PathElement.groupElement("ptMaxTrackSize"), PathElement.groupElement("x"));
	    private static final VarHandle ptMaxTrackSize_y = _MINMAXINFO.varHandle(PathElement.groupElement("ptMaxTrackSize"), PathElement.groupElement("y"));
	    public Coord ptMaxTrackSize() {return(Coord.of((int)ptMaxTrackSize_x.get(mem, 0), (int)ptMaxTrackSize_y.get(mem, 0)));}
	    public MINMAXINFO ptMaxTrackSize(Coord v) {ptMaxTrackSize_x.set(mem, 0, v.x); ptMaxTrackSize_y.set(mem, 0, v.y); return(this);}
	}

	public MINMAXINFO MINMAXINFO(long lparam) {
	    return(new MINMAXINFO(MemorySegment.ofAddress(lparam).reinterpret(_MINMAXINFO.byteSize())));
	}

	private final MethodHandle GetMessageW = ld.downcallHandle(user32.find("GetMessageW").get(), FunctionDescriptor.of(BOOL, ADDRESS, HWND, UINT, UINT));
	public boolean GetMessage(Win32.MSG lpMsg, Handle hWnd, int wMsgFilterMin, int wMsgFilterMax) {
	    int rv;
	    try {
		rv = (int)GetMessageW.invoke(lpMsg.mem(), nhandle(hWnd), wMsgFilterMin, wMsgFilterMax);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv == -1)
		throw(lasterror());
	    return(rv != 0);
	}

	private final MethodHandle PeekMessageW = ld.downcallHandle(user32.find("PeekMessageW").get(), FunctionDescriptor.of(BOOL, ADDRESS, HWND, UINT, UINT, UINT));
	public boolean PeekMessage(Win32.MSG lpMsg, Handle hWnd, int wMsgFilterMin, int wMsgFilterMax, int wRemoveMsg) {
	    int rv;
	    try {
		rv = (int)PeekMessageW.invoke(lpMsg.mem(), nhandle(hWnd), wMsgFilterMin, wMsgFilterMax, wRemoveMsg);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    return(rv != 0);
	}
	private final MethodHandle TranslateMessage = ld.downcallHandle(user32.find("TranslateMessage").get(), FunctionDescriptor.of(BOOL, ADDRESS));
	public boolean TranslateMessage(Win32.MSG lpMsg) {
	    int rv;
	    try {
		rv = (int)TranslateMessage.invoke(lpMsg.mem());
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    return(rv != 0);
	}

	private final MethodHandle DispatchMessageW = ld.downcallHandle(user32.find("DispatchMessageW").get(), FunctionDescriptor.of(LRESULT, ADDRESS));
	public long DispatchMessage(Win32.MSG lpMsg) {
	    try {
		return((long)DispatchMessageW.invoke(lpMsg.mem()));
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private final MethodHandle PostMessageW = ld.downcallHandle(user32.find("PostMessageW").get(), FunctionDescriptor.of(BOOL, HWND, UINT, WPARAM, LPARAM));
	public void PostMessage(Handle hWnd, int Msg, long wParam, long lParam) {
	    int rv;
	    try {
		rv = (int)PostMessageW.invoke(nhandle(hWnd), Msg, wParam, lParam);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv == 0)
		throw(lasterror());
	}

	private final MethodHandle SendMessageW = ld.downcallHandle(user32.find("SendMessageW").get(), FunctionDescriptor.of(BOOL, HWND, UINT, WPARAM, LPARAM));
	public int SendMessage(Handle hWnd, int Msg, long wParam, long lParam) {
	    try {
		return((int)SendMessageW.invoke(nhandle(hWnd), Msg, wParam, lParam));
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private final MethodHandle PostThreadMessageW = ld.downcallHandle(user32.find("PostThreadMessageW").get(), FunctionDescriptor.of(BOOL, DWORD, UINT, WPARAM, LPARAM));
	public void PostThreadMessage(int idThread, int Msg, long wParam, long lParam) {
	    int rv;
	    try {
		rv = (int)PostThreadMessageW.invoke(idThread, Msg, wParam, lParam);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv == 0)
		throw(lasterror());
	}

	private final MethodHandle DefWindowProcW = ld.downcallHandle(user32.find("DefWindowProcW").get(), FunctionDescriptor.of(LRESULT, HWND, UINT, WPARAM, LPARAM));
	public long DefWindowProc(Handle hWnd, int Msg, long wParam, long lParam) {
	    try {
		return((long)DefWindowProcW.invoke(nhandle(hWnd), Msg, wParam, lParam));
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private final MethodHandle ShowWindow = ld.downcallHandle(user32.find("ShowWindow").get(), FunctionDescriptor.of(BOOL, HWND, C_INT));
	public boolean ShowWindow(Handle hWnd, int nCmdShow) {
	    try {
		return((int)ShowWindow.invoke(hWnd.bits, nCmdShow) != 0);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private final MethodHandle GetWindowLongPtrW = ld.downcallHandle(user32.find("GetWindowLongPtrW").get(), FunctionDescriptor.of(LONG_PTR, HWND, C_INT));
	public long GetWindowLongPtr(Handle hWnd, int nIndex) {
	    try {
		return((long)GetWindowLongPtrW.invoke(hWnd.bits, nIndex));
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private final MethodHandle SetWindowLongPtrW = ld.downcallHandle(user32.find("SetWindowLongPtrW").get(), FunctionDescriptor.of(LONG_PTR, HWND, C_INT, LONG_PTR));
	public long SetWindowLongPtr(Handle hWnd, int nIndex, long dwNewLong) {
	    try {
		return((long)SetWindowLongPtrW.invoke(hWnd.bits, nIndex, dwNewLong));
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private final MethodHandle GetWindowRect = ld.downcallHandle(user32.find("GetWindowRect").get(), FunctionDescriptor.of(BOOL, HWND, ADDRESS));
	public Area GetWindowRect(Handle hWnd) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment buf = st.allocate(LONG, 4);
		int rv;
		try {
		    rv = (int)GetWindowRect.invoke(hWnd.bits, buf);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(rv == 0)
		    throw(lasterror());
		return(Area.corn(Coord.of((int)getint(buf, LONG.byteSize() * 0, LONG, true),
					  (int)getint(buf, LONG.byteSize() * 1, LONG, true)),
				 Coord.of((int)getint(buf, LONG.byteSize() * 2, LONG, true),
					  (int)getint(buf, LONG.byteSize() * 3, LONG, true))));
	    }
	}

	private final MethodHandle SetWindowPos = ld.downcallHandle(user32.find("SetWindowPos").get(), FunctionDescriptor.of(BOOL, HWND, HWND, C_INT, C_INT, C_INT, C_INT, UINT));
	public void SetWindowPos(Handle hWnd, Handle hWndInsertAfter, Coord pos, Coord size, int uFlags) {
	    if(hWndInsertAfter == null) uFlags |= SWP_NOZORDER;
	    if(pos == null) {uFlags |= SWP_NOMOVE; pos = Coord.z;}
	    if(size == null) {uFlags |= SWP_NOSIZE;; size = Coord.z;}
	    int rv;
	    try {
		rv = (int)SetWindowPos.invoke(hWnd.bits, nhandle(hWndInsertAfter), pos.x, pos.y, size.x, size.y, uFlags);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv == 0)
		throw(lasterror());
	}

	private final MethodHandle AdjustWindowRectEx = ld.downcallHandle(user32.find("AdjustWindowRectEx").get(), FunctionDescriptor.of(BOOL, ADDRESS, DWORD, BOOL, DWORD));
	public Area AdjustWindowRectEx(Area rect, int dwStyle, boolean bMenu, int dwExStyle) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment buf = st.allocate(LONG.byteSize() * 4);
		setint(buf, LONG.byteSize() * 0, LONG, rect.ul.x);
		setint(buf, LONG.byteSize() * 1, LONG, rect.ul.y);
		setint(buf, LONG.byteSize() * 2, LONG, rect.br.x);
		setint(buf, LONG.byteSize() * 3, LONG, rect.br.y);
		int rv;
		try {
		    rv = (int)AdjustWindowRectEx.invoke(buf, dwStyle, bMenu ? 1 : 0, dwExStyle);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(rv == 0)
		    throw(lasterror());
		return(Area.corn(Coord.of((int)getint(buf, LONG.byteSize() * 0, LONG, true),
					  (int)getint(buf, LONG.byteSize() * 1, LONG, true)),
				 Coord.of((int)getint(buf, LONG.byteSize() * 2, LONG, true),
					  (int)getint(buf, LONG.byteSize() * 3, LONG, true))));
	    }
	}

	private final MethodHandle SetWindowTextW = ld.downcallHandle(user32.find("SetWindowTextW").get(), FunctionDescriptor.of(BOOL, HWND, LPCWSTR));
	public void SetWindowText(Handle hWnd, String lpString) {
	    int rv;
	    try(Arena st = Arena.ofConfined()) {
		rv = (int)SetWindowTextW.invoke(hWnd.bits, wstr(st, lpString));
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv == 0)
		throw(lasterror());
	}

	private final MethodHandle ScreenToClient = ld.downcallHandle(user32.find("ScreenToClient").get(), FunctionDescriptor.of(BOOL, HWND, ADDRESS));
	public Coord ScreenToClient(Handle hWnd, Coord point) {
	    int rv;
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment buf = st.allocate(LONG, 2);
		setint(buf, LONG.byteSize() * 0, LONG, point.x);
		setint(buf, LONG.byteSize() * 1, LONG, point.y);
		try {
		    rv = (int)ScreenToClient.invoke(hWnd.bits, buf);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(rv == 0)
		    throw(lasterror());
		return(Coord.of((int)getint(buf, LONG.byteSize() * 0, LONG, true),
				(int)getint(buf, LONG.byteSize() * 1, LONG, true)));
	    }
	}

	private final MethodHandle ClientToScreen = ld.downcallHandle(user32.find("ClientToScreen").get(), FunctionDescriptor.of(BOOL, HWND, ADDRESS));
	public Coord ClientToScreen(Handle hWnd, Coord point) {
	    int rv;
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment buf = st.allocate(LONG, 2);
		setint(buf, LONG.byteSize() * 0, LONG, point.x);
		setint(buf, LONG.byteSize() * 1, LONG, point.y);
		try {
		    rv = (int)ClientToScreen.invoke(hWnd.bits, buf);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(rv == 0)
		    throw(lasterror());
		return(Coord.of((int)getint(buf, LONG.byteSize() * 0, LONG, true),
				(int)getint(buf, LONG.byteSize() * 1, LONG, true)));
	    }
	}

	private final MethodHandle WindowFromPoint = ld.downcallHandle(user32.find("WindowFromPoint").get(), FunctionDescriptor.of(HANDLE, _POINT));
	public Handle WindowFromPoint(Coord point) {
	    try(Arena st = Arena.ofConfined()) {
		POINT buf = new POINT(st);
		buf.c(point);
		MemorySegment rv;
		try {
		    rv = (MemorySegment)WindowFromPoint.invoke(buf.mem());
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		return(Handle.of(rv));
	    }
	}

	private static int EnumDisplayMonitors(List<Handle> buf, MemorySegment hMonitor, MemorySegment hdcMonitor, MemorySegment lprcMonitor, long dwData) {
	    buf.add(Handle.of(hMonitor));
	    return(1);
	}
	static final MethodHandle _EnumDisplayMonitors = slookup(MethodHandles.lookup(), Win64Unicode.class, "EnumDisplayMonitors", Integer.TYPE,
								 List.class, MemorySegment.class, MemorySegment.class, MemorySegment.class, Long.TYPE);

	private final MethodHandle EnumDisplayMonitors = ld.downcallHandle(user32.find("EnumDisplayMonitors").get(), FunctionDescriptor.of(BOOL, HDC, ADDRESS, ADDRESS, LPARAM));
	public List<Handle> EnumDisplayMonitors() {
	    try(Arena st = Arena.ofConfined()) {
		List<Handle> buf = new ArrayList<>();
		MethodHandle proc = MethodHandles.insertArguments(_EnumDisplayMonitors, 0, buf);
		MemorySegment stub = ld.upcallStub(proc, FunctionDescriptor.of(BOOL, HMONITOR, HDC, ADDRESS, LPARAM), st);
		int rv;
		try {
		    rv = (int)EnumDisplayMonitors.invoke(MemorySegment.NULL, MemorySegment.NULL, stub, 0);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(rv == 0)
		    throw(lasterror());
		return(buf);
	    }
	}

	private final MethodHandle MonitorFromWindow = ld.downcallHandle(user32.find("MonitorFromWindow").get(), FunctionDescriptor.of(HMONITOR, HWND, DWORD));
	public Handle MonitorFromWindow(Handle hWnd, int dwFlags) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)MonitorFromWindow.invoke(hWnd.bits, dwFlags);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    return(nullp(rv) ? null : Handle.of(rv));
	}

	static final StructLayout _MONITORINFO = struct(new MemoryLayout[] {
		DWORD.withName("cbSize"),
		_RECT.withName("rcMonitor"),
		_RECT.withName("rcWork"),
		DWORD.withName("dwFlags"),
	    });
	public static class MONITORINFO extends Win32.MONITORINFO {
	    private MONITORINFO(Arena alloc) {
		super(alloc.allocate(_MONITORINFO.byteSize()));
		cbSize.set(mem, 0, (int)_MONITORINFO.byteSize());
	    }
	    protected MONITORINFO() {
		this(Arena.ofAuto());
	    }

	    protected StructLayout $layout() {return(_MONITORINFO);}

	    private static final VarHandle cbSize = _MONITORINFO.varHandle(PathElement.groupElement("cbSize"));
	    private static final VarHandle rcMonitor_left = _MONITORINFO.varHandle(PathElement.groupElement("rcMonitor"), PathElement.groupElement("left"));
	    private static final VarHandle rcMonitor_top = _MONITORINFO.varHandle(PathElement.groupElement("rcMonitor"), PathElement.groupElement("top"));
	    private static final VarHandle rcMonitor_right = _MONITORINFO.varHandle(PathElement.groupElement("rcMonitor"), PathElement.groupElement("right"));
	    private static final VarHandle rcMonitor_bottom = _MONITORINFO.varHandle(PathElement.groupElement("rcMonitor"), PathElement.groupElement("bottom"));
	    public Area rcMonitor() {
		return(Area.corn(Coord.of((int)rcMonitor_left.get(mem, 0), (int)rcMonitor_top.get(mem, 0)),
				 Coord.of((int)rcMonitor_right.get(mem, 0), (int)rcMonitor_bottom.get(mem, 0))));
	    }
	    private static final VarHandle rcWork_left = _MONITORINFO.varHandle(PathElement.groupElement("rcWork"), PathElement.groupElement("left"));
	    private static final VarHandle rcWork_top = _MONITORINFO.varHandle(PathElement.groupElement("rcWork"), PathElement.groupElement("top"));
	    private static final VarHandle rcWork_right = _MONITORINFO.varHandle(PathElement.groupElement("rcWork"), PathElement.groupElement("right"));
	    private static final VarHandle rcWork_bottom = _MONITORINFO.varHandle(PathElement.groupElement("rcWork"), PathElement.groupElement("bottom"));
	    public Area rcWork() {
		return(Area.corn(Coord.of((int)rcWork_left.get(mem, 0), (int)rcWork_top.get(mem, 0)),
				 Coord.of((int)rcWork_right.get(mem, 0), (int)rcWork_bottom.get(mem, 0))));
	    }
	    private static final VarHandle dwFlags = _MONITORINFO.varHandle(PathElement.groupElement("dwFlags"));
	    public int dwFlags() {return((int)dwFlags.get(mem, 0));}
	}
	public MONITORINFO MONITORINFO() {return(new MONITORINFO());}

	private final MethodHandle GetMonitorInfoW = ld.downcallHandle(user32.find("GetMonitorInfoW").get(), FunctionDescriptor.of(BOOL, HMONITOR, ADDRESS));
	public void GetMonitorInfo(Handle hMonitor, Win32.MONITORINFO lpmi) {
	    int rv;
	    try {
		rv = (int)GetMonitorInfoW.invoke(hMonitor.bits, lpmi.mem());
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv == 0)
		throw(lasterror());
	}

	private final MethodHandle GetDC = ld.downcallHandle(user32.find("GetDC").get(), FunctionDescriptor.of(HDC, HWND));
	public Handle GetDC(Handle hWnd) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)GetDC.invoke(nhandle(hWnd));
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(nullp(rv))
		throw(lasterror());
	    return(Handle.of(rv));
	}

	private final MethodHandle ReleaseDC = ld.downcallHandle(user32.find("ReleaseDC").get(), FunctionDescriptor.of(C_INT, HDC, HWND));
	public boolean ReleaseDC(Handle hWnd, Handle hDC) {
	    try {
		return((int)ReleaseDC.invoke(nhandle(hWnd), hDC.bits) != 0);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private final MethodHandle GetDeviceCaps = ld.downcallHandle(gdi32.find("GetDeviceCaps").get(), FunctionDescriptor.of(C_INT, HDC, C_INT));
	public int GetDeviceCaps(Handle hdc, int index) {
	    try {
		return((int)GetDeviceCaps.invoke(hdc.bits, index));
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private final MethodHandle GetSystemMetrics = ld.downcallHandle(user32.find("GetSystemMetrics").get(), FunctionDescriptor.of(C_INT, C_INT));
	public int GetSystemMetrics(int nIndex) {
	    try {
		return((int)GetSystemMetrics.invoke(nIndex));
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private final MethodHandle GetKeyState = ld.downcallHandle(user32.find("GetKeyState").get(), FunctionDescriptor.of(SHORT, C_INT));
	public int GetKeyState(int nVirtKey) {
	    try {
		return((int)GetKeyState.invoke(nVirtKey));
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private final MethodHandle GetKeyboardState = ld.downcallHandle(user32.find("GetKeyboardState").get(), FunctionDescriptor.of(BOOL, ADDRESS));
	public void GetKeyboardState(KeyboardState buf) {
	    int rv;
	    try {
		rv = (int)GetKeyboardState.invoke(buf.keys);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv == 0)
		throw(lasterror());
	}

	private final MethodHandle GetKeyboardLayout = ld.downcallHandle(user32.find("GetKeyboardLayout").get(), FunctionDescriptor.of(HKL, DWORD));
	public Handle GetKeyboardLayout(int idThread) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)GetKeyboardLayout.invoke(idThread);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    return(Handle.of(rv));
	}

	private final MethodHandle GetKeyboardLayoutList = ld.downcallHandle(user32.find("GetKeyboardLayoutList").get(), FunctionDescriptor.of(C_INT, C_INT, ADDRESS));
	public Handle[] GetKeyboardLayoutList() {
	    for(int n = 16; n < (1 << 30); n <<= 1) {
		try(Arena st = Arena.ofConfined()) {
		    MemorySegment buf = st.allocate(HKL, n);
		    int rv;
		    try {
			rv = (int)GetKeyboardLayoutList.invoke(n, buf);
		    } catch(Throwable e) {
			throw(new InvocationException(e));
		    }
		    if(rv == 0)
			throw(lasterror());
		    if(rv < n) {
			Handle[] ret = new Handle[rv];
			for(int i = 0; i < rv; i++)
			    ret[i] = Handle.of(buf.get(ADDRESS, i * HKL.byteSize()));
			return(ret);
		    }
		}
	    }
	    throw(new RuntimeException());
	}

	private final MethodHandle MapVirtualKeyExW = ld.downcallHandle(user32.find("MapVirtualKeyExW").get(), FunctionDescriptor.of(UINT, UINT, UINT, HKL));
	public int MapVirtualKeyEx(int uCode, int uMapType, Handle dwhkl) {
	    try {
		return((int)MapVirtualKeyExW.invoke(uCode, uMapType, dwhkl.bits));
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private final MethodHandle VkKeyScanExW = ld.downcallHandle(user32.find("VkKeyScanExW").get(), FunctionDescriptor.of(SHORT, WCHAR, HKL));
	public int VkKeyScanEx(int ch, Handle dwhkl) {
	    try {
		return((short)VkKeyScanExW.invoke((short)ch, dwhkl.bits) & 0xffff);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private final MethodHandle ToUnicodeEx = ld.downcallHandle(user32.find("ToUnicodeEx").get(), FunctionDescriptor.of(C_INT, UINT, UINT, ADDRESS, LPWSTR, C_INT, UINT, HKL));
	public char[] ToUnicodeEx(int wVirtKey, int wScanCode, KeyboardState lpKeyState, int wFlags, Handle dwhkl) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment buf = st.allocate(WCHAR.byteSize() * 128);
		int rv;
		try {
		    rv = (int)ToUnicodeEx.invoke(wVirtKey, wScanCode, lpKeyState.keys, buf, 128, wFlags, dwhkl.bits);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(rv < 0)
		    return(null);
		char[] ret = new char[rv];
		for(int i = 0; i < rv; i++)
		    ret[i] = (char)getint(buf, WCHAR.byteSize() * i, WCHAR, false);
		return(ret);
	    }
	}

	private final MethodHandle GetKeyNameTextW = ld.downcallHandle(user32.find("GetKeyNameTextW").get(), FunctionDescriptor.of(C_INT, LONG, LPWSTR, C_INT));
	public String GetKeyNameText(long lParam) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment buf = st.allocate(WCHAR.byteSize() * 128);
		int rv;
		try {
		    rv = (int)GetKeyNameTextW.invoke((int)lParam, buf, 128);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(rv <= 0)
		    return(null);
		char[] ret = new char[rv];
		for(int i = 0; i < rv; i++)
		    ret[i] = (char)getint(buf, WCHAR.byteSize() * i, WCHAR, false);
		return(new String(ret));
	    }
	}

	static final MemoryLayout FXPT2DOT30 = LONG;
	static final StructLayout CIEXYZ = struct(new MemoryLayout[] {
		FXPT2DOT30.withName("ciexyzX"),
		FXPT2DOT30.withName("ciexyzY"),
		FXPT2DOT30.withName("ciexyzZ"),
	    });
	static final StructLayout CIEXYZTRIPLE = struct(new MemoryLayout[] {
		CIEXYZ.withName("ciexyzRed"),
		CIEXYZ.withName("ciexyzGreen"),
		CIEXYZ.withName("ciexyzBlue"),
	    });
	static final StructLayout _BITMAPV5HEADER = struct(new MemoryLayout[] {
		DWORD.withName("bV5Size"),
		LONG.withName("bV5Width"),
		LONG.withName("bV5Height"),
		WORD.withName("bV5Planes"),
		WORD.withName("bV5BitCount"),
		DWORD.withName("bV5Compression"),
		DWORD.withName("bV5SizeImage"),
		LONG.withName("bV5XPelsPerMeter"),
		LONG.withName("bV5YPelsPerMeter"),
		DWORD.withName("bV5ClrUsed"),
		DWORD.withName("bV5ClrImportant"),
		DWORD.withName("bV5RedMask"),
		DWORD.withName("bV5GreenMask"),
		DWORD.withName("bV5BlueMask"),
		DWORD.withName("bV5AlphaMask"),
		DWORD.withName("bV5CSType"),
		CIEXYZTRIPLE.withName("bV5Endpoints"),
		DWORD.withName("bV5GammaRed"),
		DWORD.withName("bV5GammaGreen"),
		DWORD.withName("bV5GammaBlue"),
		DWORD.withName("bV5Intent"),
		DWORD.withName("bV5ProfileData"),
		DWORD.withName("bV5ProfileSize"),
		DWORD.withName("bV5Reserved"),
	    });
	public static class BITMAPV5HEADER extends Win32.BITMAPV5HEADER {
	    private BITMAPV5HEADER(MemorySegment mem) {
		super(mem);
	    }
	    private BITMAPV5HEADER(Arena alloc) {
		this(alloc.allocate(_BITMAPV5HEADER.byteSize()));
		bV5Size.set(mem, 0, (int)_BITMAPV5HEADER.byteSize());
	    }
	    protected BITMAPV5HEADER() {
		this(Arena.ofAuto());
	    }

	    protected StructLayout $layout() {return(_BITMAPV5HEADER);}
	    public long sizeof() {return($layout().byteSize());}

	    private static final VarHandle bV5Size = _BITMAPV5HEADER.varHandle(PathElement.groupElement("bV5Size"));
	    public BITMAPV5HEADER bV5Size(int v) {bV5Size.set(mem, 0, v); return(this);}
	    public int bV5Size() {return((int)bV5Size.get(mem, 0));}
	    private static final VarHandle bV5Width = _BITMAPV5HEADER.varHandle(PathElement.groupElement("bV5Width"));
	    public BITMAPV5HEADER bV5Width(int v) {bV5Width.set(mem, 0, v); return(this);}
	    public int bV5Width() {return((int)bV5Width.get(mem, 0));}
	    private static final VarHandle bV5Height = _BITMAPV5HEADER.varHandle(PathElement.groupElement("bV5Height"));
	    public BITMAPV5HEADER bV5Height(int v) {bV5Height.set(mem, 0, v); return(this);}
	    public int bV5Height() {return((int)bV5Height.get(mem, 0));}
	    private static final VarHandle bV5Planes = _BITMAPV5HEADER.varHandle(PathElement.groupElement("bV5Planes"));
	    public BITMAPV5HEADER bV5Planes(int v) {bV5Planes.set(mem, 0, (short)v); return(this);}
	    private static final VarHandle bV5BitCount = _BITMAPV5HEADER.varHandle(PathElement.groupElement("bV5BitCount"));
	    public BITMAPV5HEADER bV5BitCount(int v) {bV5BitCount.set(mem, 0, (short)v); return(this);}
	    public int bV5BitCount() {return((short)bV5BitCount.get(mem, 0));}
	    private static final VarHandle bV5Compression = _BITMAPV5HEADER.varHandle(PathElement.groupElement("bV5Compression"));
	    public BITMAPV5HEADER bV5Compression(int v) {bV5Compression.set(mem, 0, v); return(this);}
	    public int bV5Compression() {return((int)bV5Compression.get(mem, 0));}
	    private static final VarHandle bV5SizeImage = _BITMAPV5HEADER.varHandle(PathElement.groupElement("bV5SizeImage"));
	    public BITMAPV5HEADER bV5SizeImage(int v) {bV5SizeImage.set(mem, 0, v); return(this);}
	    public int bV5SizeImage() {return((int)bV5SizeImage.get(mem, 0));}
	    private static final VarHandle bV5ClrUsed = _BITMAPV5HEADER.varHandle(PathElement.groupElement("bV5ClrUsed"));
	    public BITMAPV5HEADER bV5ClrUsed(int v) {bV5ClrUsed.set(mem, 0, v); return(this);}
	    public int bV5ClrUsed() {return((int)bV5ClrUsed.get(mem, 0));}
	    private static final VarHandle bV5RedMask = _BITMAPV5HEADER.varHandle(PathElement.groupElement("bV5RedMask"));
	    public BITMAPV5HEADER bV5RedMask(int v) {bV5RedMask.set(mem, 0, v); return(this);}
	    public int bV5RedMask() {return((int)bV5RedMask.get(mem, 0));}
	    private static final VarHandle bV5GreenMask = _BITMAPV5HEADER.varHandle(PathElement.groupElement("bV5GreenMask"));
	    public BITMAPV5HEADER bV5GreenMask(int v) {bV5GreenMask.set(mem, 0, v); return(this);}
	    public int bV5GreenMask() {return((int)bV5GreenMask.get(mem, 0));}
	    private static final VarHandle bV5BlueMask = _BITMAPV5HEADER.varHandle(PathElement.groupElement("bV5BlueMask"));
	    public BITMAPV5HEADER bV5BlueMask(int v) {bV5BlueMask.set(mem, 0, v); return(this);}
	    public int bV5BlueMask() {return((int)bV5BlueMask.get(mem, 0));}
	    private static final VarHandle bV5AlphaMask = _BITMAPV5HEADER.varHandle(PathElement.groupElement("bV5AlphaMask"));
	    public BITMAPV5HEADER bV5AlphaMask(int v) {bV5AlphaMask.set(mem, 0, v); return(this);}
	    public int bV5AlphaMask() {return((int)bV5AlphaMask.get(mem, 0));}
	}
	public int sizeof_BITMAPV5HEADER() {return((int)_BITMAPV5HEADER.byteSize());}
	public BITMAPV5HEADER BITMAPV5HEADER() {return(new BITMAPV5HEADER());}
	public BITMAPV5HEADER BITMAPV5HEADER(MemorySegment mem) {return(new BITMAPV5HEADER(mem));}

	static final StructLayout _DROPFILES = struct(new MemoryLayout[] {
		DWORD.withName("pFiles"),
		_POINT.withName("pt"),
		BOOL.withName("fNC"),
		BOOL.withName("fWide"),
	    });
	public static class DROPFILES extends Win32.DROPFILES {
	    private DROPFILES(MemorySegment mem) {
		super(mem);
	    }
	    private DROPFILES(Arena alloc, int extra) {
		this(alloc.allocate(_DROPFILES.byteSize() + extra));
		pFiles.set(mem, 0, (int)_DROPFILES.byteSize());
	    }
	    protected DROPFILES(int extra) {
		this(Arena.ofAuto(), extra);
	    }

	    protected StructLayout $layout() {return(_DROPFILES);}
	    public long sizeof() {return($layout().byteSize());}

	    private static final VarHandle pFiles = _DROPFILES.varHandle(PathElement.groupElement("pFiles"));
	    public DROPFILES pFiles(int v) {pFiles.set(mem, 0, v); return(this);}
	    public int pFiles() {return((int)pFiles.get(mem, 0));}
	    private static final VarHandle pt_x = _DROPFILES.varHandle(PathElement.groupElement("pt"), PathElement.groupElement("x"));
	    private static final VarHandle pt_y = _DROPFILES.varHandle(PathElement.groupElement("pt"), PathElement.groupElement("y"));
	    public DROPFILES pt(Coord v) {pt_x.set(mem, 0, v.x); pt_y.set(mem, 0, v.y); return(this);}
	    public Coord pt() {return(Coord.of((int)pt_x.get(mem, 0), (int)pt_y.get(mem, 0)));}
	    private static final VarHandle fNC = _DROPFILES.varHandle(PathElement.groupElement("fNC"));
	    public DROPFILES fNC(boolean v) {fNC.set(mem, 0, v ? 1 : 0); return(this);}
	    public boolean fNC() {return(((int)fNC.get(mem, 0)) != 0);}
	    private static final VarHandle fWide = _DROPFILES.varHandle(PathElement.groupElement("fWide"));
	    public DROPFILES fWide(boolean v) {fWide.set(mem, 0, v ? 1 : 0); return(this);}
	    public boolean fWide() {return(((int)fWide.get(mem, 0)) != 0);}
	}
	public int sizeof_DROPFILES() {return((int)_DROPFILES.byteSize());}
	public DROPFILES DROPFILES(int extra) {return(new DROPFILES(extra));}
	public DROPFILES DROPFILES(MemorySegment mem) {return(new DROPFILES(mem));}

	static final StructLayout _ICONINFO = struct(new MemoryLayout[] {
		BOOL.withName("fIcon"),
		DWORD.withName("xHotspot"),
		DWORD.withName("yHotspot"),
		HBITMAP.withName("hbmMask"),
		HBITMAP.withName("hbmColor"),
	    });
	public static class ICONINFO extends Win32.ICONINFO {
	    private ICONINFO(Arena alloc) {
		super(alloc.allocate(_ICONINFO.byteSize()));
	    }
	    protected ICONINFO() {
		this(Arena.ofAuto());
	    }

	    protected StructLayout $layout() {return(_ICONINFO);}

	    private static final VarHandle fIcon = _ICONINFO.varHandle(PathElement.groupElement("fIcon"));
	    public ICONINFO fIcon(int v) {fIcon.set(mem, 0, v); return(this);}
	    private static final VarHandle xHotspot = _ICONINFO.varHandle(PathElement.groupElement("xHotspot"));
	    public ICONINFO xHotspot(int v) {xHotspot.set(mem, 0, v); return(this);}
	    private static final VarHandle yHotspot = _ICONINFO.varHandle(PathElement.groupElement("yHotspot"));
	    public ICONINFO yHotspot(int v) {xHotspot.set(mem, 0, v); return(this);}
	    private static final VarHandle hbmMask = _ICONINFO.varHandle(PathElement.groupElement("hbmMask"));
	    public ICONINFO hbmMask(Handle v) {hbmMask.set(mem, 0, nhandle(v)); return(this);}
	    private static final VarHandle hbmColor = _ICONINFO.varHandle(PathElement.groupElement("hbmColor"));
	    public ICONINFO hbmColor(Handle v) {hbmColor.set(mem, 0, nhandle(v)); return(this);}
	}
	public ICONINFO ICONINFO() {return(new ICONINFO());}

	private final MethodHandle CreateDIBSection = ld.downcallHandle(gdi32.find("CreateDIBSection").get(), FunctionDescriptor.of(HBITMAP, HDC, ADDRESS, UINT, ADDRESS, HANDLE, DWORD));
	public Handle CreateDIBSection(Handle hDC, Win32.BITMAPV5HEADER pbmi, int usage, ByteBuffer[] ppvBits, Handle hSection, int offset) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment bitsbuf = st.allocate(ADDRESS);
		MemorySegment rv;
		try {
		    rv = (MemorySegment)CreateDIBSection.invoke(hDC.bits, pbmi.mem(), usage, bitsbuf, nhandle(hSection), offset);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(nullp(rv))
		    throw(lasterror());
		if(ppvBits != null)
		    ppvBits[0] = bitsbuf.get(ADDRESS, 0).reinterpret(pbmi.bV5Width() * Math.abs(pbmi.bV5Height()) * pbmi.bV5BitCount() / 8).asByteBuffer();
		return(Handle.of(rv));
	    }
	}

	private final MethodHandle CreateBitmap = ld.downcallHandle(gdi32.find("CreateBitmap").get(), FunctionDescriptor.of(HBITMAP, C_INT, C_INT, UINT, UINT, ADDRESS));
	public Handle CreateBitmap(int nWidth, int nHeight, int nPlanes, int nBitCount, ByteBuffer lpBits) {
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment bits = bufcpy(st, lpBits, nWidth * nHeight * nBitCount / 8);
		MemorySegment rv;
		try {
		    rv = (MemorySegment)CreateBitmap.invoke(nWidth, nHeight, nPlanes, nBitCount, bits);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(nullp(rv))
		    throw(lasterror());
		return(Handle.of(rv));
	    }
	}

	private final MethodHandle CreateIconIndirect = ld.downcallHandle(user32.find("CreateIconIndirect").get(), FunctionDescriptor.of(HICON, ADDRESS));
	public Handle CreateIconIndirect(Win32.ICONINFO piconinfo) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)CreateIconIndirect.invoke(piconinfo.mem());
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(nullp(rv))
		throw(lasterror());
	    return(Handle.of(rv));
	}

	private final MethodHandle SetCursor = ld.downcallHandle(user32.find("SetCursor").get(), FunctionDescriptor.of(HCURSOR, HCURSOR));
	public Handle SetCursor(Handle hCursor) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)SetCursor.invoke(nhandle(hCursor));
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    return(nullp(rv) ? null : Handle.of(rv));
	}

	private final MethodHandle LoadCursorW = ld.downcallHandle(user32.find("LoadCursorW").get(), FunctionDescriptor.of(HCURSOR, HINSTANCE, ADDRESS));
	public Handle LoadCursor(Handle hInstance, int cursor) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)LoadCursorW.invoke(nhandle(hInstance), MemorySegment.ofAddress(cursor & 0xffff));
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(nullp(rv))
		throw(lasterror());
	    return(Handle.of(rv));
	}

	private final MethodHandle DestroyIcon = ld.downcallHandle(user32.find("DestroyIcon").get(), FunctionDescriptor.of(BOOL, HICON));
	public void DestroyIcon(Handle ho) {
	    int rv;
	    try {
		rv = (int)DestroyIcon.invoke(ho.bits);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv == 0)
		throw(lasterror());
	}

	private final MethodHandle RegisterClipboardFormatW = ld.downcallHandle(user32.find("RegisterClipboardFormatW").get(), FunctionDescriptor.of(UINT, ADDRESS));
	public int RegisterClipboardFormat(String format) {
	    int rv;
	    try(Arena st = Arena.ofConfined()) {
		rv = (int)RegisterClipboardFormatW.invoke(st.allocateFrom(format, C_WCHARSET));
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv == 0)
		throw(lasterror());
	    return(rv);
	}

	private final MethodHandle GetClipboardFormatNameW = ld.downcallHandle(user32.find("GetClipboardFormatNameW").get(), FunctionDescriptor.of(C_INT, UINT, ADDRESS, C_INT));
	public String GetClipboardFormatName(int format) {
	    try(Arena st = Arena.ofConfined()) {
		int max = 256;
		MemorySegment buf = st.allocate(C_SHORT, max);
		int rv;
		try {
		    rv = (int)GetClipboardFormatNameW.invoke(format, buf, max);
		} catch(Throwable e) {
		    throw(new InvocationException(e));
		}
		if(rv == 0)
		    throw(lasterror());
		return(C_WCHARSET.decode(buf.asSlice(0, rv * 2).asByteBuffer()).toString());
	    }
	}

	private final MethodHandle OpenClipboard = ld.downcallHandle(user32.find("OpenClipboard").get(), FunctionDescriptor.of(BOOL, HWND));
	public void OpenClipboard(Handle newowner) {
	    int rv;
	    try {
		rv = (int)OpenClipboard.invoke(newowner.bits);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv == 0)
		throw(lasterror());
	}

	private final MethodHandle CloseClipboard = ld.downcallHandle(user32.find("CloseClipboard").get(), FunctionDescriptor.of(BOOL));
	public void CloseClipboard() {
	    int rv;
	    try {
		rv = (int)CloseClipboard.invoke();
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv == 0)
		throw(lasterror());
	}

	private final MethodHandle IsClipboardFormatAvailable = ld.downcallHandle(user32.find("IsClipboardFormatAvailable").get(), FunctionDescriptor.of(BOOL, UINT));
	public boolean IsClipboardFormatAvailable(int format) {
	    try {
		return((int)IsClipboardFormatAvailable.invoke(format) != 0);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	}

	private final MethodHandle GetClipboardData = ld.downcallHandle(user32.find("GetClipboardData").get(), FunctionDescriptor.of(HANDLE, UINT));
	public Handle GetClipboardData(int format) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)GetClipboardData.invoke(format);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(nullp(rv))
		throw(lasterror());
	    return(Handle.of(rv));
	}

	private final MethodHandle EmptyClipboard = ld.downcallHandle(user32.find("EmptyClipboard").get(), FunctionDescriptor.of(BOOL));
	public void EmptyClipboard() {
	    int rv;
	    try {
		rv = (int)EmptyClipboard.invoke();
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv == 0)
		throw(lasterror());
	}

	private final MethodHandle SetClipboardData = ld.downcallHandle(user32.find("SetClipboardData").get(), FunctionDescriptor.of(HANDLE, UINT, HANDLE));
	public void SetClipboardData(int format, Handle mem) {
	    MemorySegment rv;
	    try {
		rv = (MemorySegment)SetClipboardData.invoke(format, mem.bits);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(nullp(rv))
		throw(lasterror());
	}

	private final MethodHandle RegGetValueW = ld.downcallHandle(advapi32.find("RegGetValueW").get(), FunctionDescriptor.of(LSTATUS, HKEY, LPCWSTR, LPCWSTR, DWORD, ADDRESS, ADDRESS, ADDRESS));
	public Object RegGetValue(Handle hKey, String lpSubKey, String lpValue, int dwFlags) {
	    if((dwFlags & RRF_RT_ANY) == 0)
		dwFlags |= RRF_RT_ANY;
	    try(Arena st = Arena.ofConfined()) {
		MemorySegment dbuf = st.allocate(256);
		MemorySegment tbuf = st.allocate(DWORD), lbuf = st.allocate(DWORD);
		setint(lbuf, 0, DWORD, dbuf.byteSize());
		int rv;
		try {
		    rv = (int)(long)RegGetValueW.invoke(hKey.bits, st.allocateFrom(lpSubKey, C_WCHARSET), st.allocateFrom(lpValue, C_WCHARSET),
							dwFlags, tbuf, dbuf, lbuf);
		} catch(Throwable e) {throw(new InvocationException(e));}
		if(rv == ERROR_MORE_DATA) {
		    try {
			dbuf = st.allocate(getint(lbuf, 0, DWORD, true));
			setint(lbuf, 0, DWORD, dbuf.byteSize());
			rv = (int)(long)RegGetValueW.invoke(hKey.bits, st.allocateFrom(lpSubKey, C_WCHARSET), st.allocateFrom(lpValue, C_WCHARSET),
							    dwFlags, tbuf, dbuf, lbuf);
		    } catch(Throwable e) {throw(new InvocationException(e));}
		}
		if(rv != ERROR_SUCCESS)
		    throw(new StdError(rv));
		int len = (int)getint(lbuf, 0, DWORD, true);
		int type = (int)getint(tbuf, 0, DWORD, true);
		switch(type) {
		case REG_SZ: return(dbuf.getString(0, C_WCHARSET));
		case REG_BINARY: return(memcpy(dbuf, 0, len));
		case REG_DWORD: return((int)getint(dbuf, 0, DWORD, true));
		default: throw(new RuntimeException("unhandled registry value type: " + type));
		}
	    }
	}

	private final MethodHandle DeleteObject = ld.downcallHandle(gdi32.find("DeleteObject").get(), FunctionDescriptor.of(BOOL, HGDIOBJ));
	public void DeleteObject(Handle ho) {
	    int rv;
	    try {
		rv = (int)DeleteObject.invoke(ho.bits);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv == 0)
		throw(lasterror());
	}

	static final StructLayout _SHELLEXECUTEINFOW = struct(new MemoryLayout[] {
		DWORD.withName("cbSize"),
		ULONG.withName("fMask"),
		HWND.withName("hWnd"),
		LPCWSTR.withName("lpVerb"),
		LPCWSTR.withName("lpFile"),
		LPCWSTR.withName("lpParameters"),
		LPCWSTR.withName("lpDirectory"),
		C_INT.withName("nShow"),
		HINSTANCE.withName("hInstApp"),
		ADDRESS.withName("lpIDList"),
		LPCWSTR.withName("lpClass"),
		HKEY.withName("hkeyClass"),
		DWORD.withName("dwHotKey"),
		HANDLE.withName("hIcon"),
		HANDLE.withName("hProcess"),
	    });
	public static class SHELLEXECUTEINFOW extends Win32.SHELLEXECUTEINFO {
	    private final Arena alloc;

	    private SHELLEXECUTEINFOW(Arena alloc) {
		super(alloc.allocate(_SHELLEXECUTEINFOW.byteSize()));
		this.alloc = alloc;
		cbSize.set(mem, 0, (int)_SHELLEXECUTEINFOW.byteSize());
	    }
	    protected SHELLEXECUTEINFOW() {
		this(Arena.ofAuto());
	    }

	    protected StructLayout $layout() {return(_SHELLEXECUTEINFOW);}

	    private static final VarHandle cbSize = _SHELLEXECUTEINFOW.varHandle(PathElement.groupElement("cbSize"));
	    private static final VarHandle lpVerb = _SHELLEXECUTEINFOW.varHandle(PathElement.groupElement("lpVerb"));
	    public SHELLEXECUTEINFOW lpVerb(String value) {
		lpVerb.set(mem, 0, wstr(alloc, value));
		return(this);
	    }
	    private static final VarHandle lpFile = _SHELLEXECUTEINFOW.varHandle(PathElement.groupElement("lpFile"));
	    public SHELLEXECUTEINFOW lpFile(String value) {
		lpFile.set(mem, 0, wstr(alloc, value));
		return(this);
	    }
	    private static final VarHandle lpParameters = _SHELLEXECUTEINFOW.varHandle(PathElement.groupElement("lpParameters"));
	    public SHELLEXECUTEINFOW lpParameters(String value) {
		lpParameters.set(mem, 0, wstr(alloc, value));
		return(this);
	    }
	    private static final VarHandle lpDirectory = _SHELLEXECUTEINFOW.varHandle(PathElement.groupElement("lpDirectory"));
	    public SHELLEXECUTEINFOW lpDirectory(String value) {
		lpDirectory.set(mem, 0, wstr(alloc, value));
		return(this);
	    }
	    private static final VarHandle nShow = _SHELLEXECUTEINFOW.varHandle(PathElement.groupElement("nShow"));
	    public SHELLEXECUTEINFOW nShow(int value) {
		nShow.set(mem, 0, value);
		return(this);
	    }
	}
	public SHELLEXECUTEINFOW SHELLEXECUTEINFO() {return(new SHELLEXECUTEINFOW());}

	private final MethodHandle ShellExecuteExW = ld.downcallHandle(shell32.find("ShellExecuteExW").get(), FunctionDescriptor.of(BOOL, ADDRESS));
	public void ShellExecuteEx(Win32.SHELLEXECUTEINFO pExecInfo) {
	    int rv;
	    try {
		rv = (int)ShellExecuteExW.invoke(pExecInfo.mem());
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv == 0)
		throw(lasterror());
	}

	static final StructLayout _PIXELFORMATDESCRIPTOR = struct(new MemoryLayout[] {
		WORD.withName("nSize"),
		WORD.withName("nVersion"),
		DWORD.withName("dwFlags"),
		BYTE.withName("iPixelType"),
		BYTE.withName("cColorBits"),
		BYTE.withName("cRedBits"),
		BYTE.withName("cRedShift"),
		BYTE.withName("cGreenBits"),
		BYTE.withName("cGreenShift"),
		BYTE.withName("cBlueBits"),
		BYTE.withName("cBlueShift"),
		BYTE.withName("cAlphaBits"),
		BYTE.withName("cAlphaShift"),
		BYTE.withName("cAccumBits"),
		BYTE.withName("cAccumRedBits"),
		BYTE.withName("cAccumGreenBits"),
		BYTE.withName("cAccumBlueBits"),
		BYTE.withName("cAccumAlphaBits"),
		BYTE.withName("cDepthBits"),
		BYTE.withName("cStencilBits"),
		BYTE.withName("cAuxBuffers"),
		BYTE.withName("iLayerType"),
		BYTE.withName("bReserved"),
		DWORD.withName("dwLayerMask"),
		DWORD.withName("dwVisibleMask"),
		DWORD.withName("dwDamageMask"),
	    });
	public static class PIXELFORMATDESCRIPTOR extends Win32.PIXELFORMATDESCRIPTOR {
	    private PIXELFORMATDESCRIPTOR(Arena alloc) {
		super(alloc.allocate(_PIXELFORMATDESCRIPTOR.byteSize()));
		nSize.set(mem, 0, (short)_PIXELFORMATDESCRIPTOR.byteSize());
	    }
	    protected PIXELFORMATDESCRIPTOR() {
		this(Arena.ofAuto());
	    }

	    protected StructLayout $layout() {return(_PIXELFORMATDESCRIPTOR);}

	    private static final VarHandle nSize = _PIXELFORMATDESCRIPTOR.varHandle(PathElement.groupElement("nSize"));
	    private static final VarHandle nVersion = _PIXELFORMATDESCRIPTOR.varHandle(PathElement.groupElement("nVersion"));
	    public PIXELFORMATDESCRIPTOR nVersion(int value) {nVersion.set(mem, 0, (short)value); return(this);}
	    private static final VarHandle dwFlags = _PIXELFORMATDESCRIPTOR.varHandle(PathElement.groupElement("dwFlags"));
	    public PIXELFORMATDESCRIPTOR dwFlags(int value) {dwFlags.set(mem, 0, value); return(this);}
	    private static final VarHandle iPixelType = _PIXELFORMATDESCRIPTOR.varHandle(PathElement.groupElement("iPixelType"));
	    public PIXELFORMATDESCRIPTOR iPixelType(int value) {iPixelType.set(mem, 0, (byte)value); return(this);}
	    private static final VarHandle cColorBits = _PIXELFORMATDESCRIPTOR.varHandle(PathElement.groupElement("cColorBits"));
	    public PIXELFORMATDESCRIPTOR cColorBits(int value) {cColorBits.set(mem, 0, (byte)value); return(this);}
	    private static final VarHandle cAlphaBits = _PIXELFORMATDESCRIPTOR.varHandle(PathElement.groupElement("cAlphaBits"));
	    public PIXELFORMATDESCRIPTOR cAlphaBits(int value) {cAlphaBits.set(mem, 0, (byte)value); return(this);}
	    private static final VarHandle cAccumBits = _PIXELFORMATDESCRIPTOR.varHandle(PathElement.groupElement("cAccumBits"));
	    public PIXELFORMATDESCRIPTOR cAccumBits(int value) {cAccumBits.set(mem, 0, (byte)value); return(this);}
	    private static final VarHandle cDepthBits = _PIXELFORMATDESCRIPTOR.varHandle(PathElement.groupElement("cDepthBits"));
	    public PIXELFORMATDESCRIPTOR cDepthBits(int value) {cDepthBits.set(mem, 0, (byte)value); return(this);}
	    private static final VarHandle cStencilBits = _PIXELFORMATDESCRIPTOR.varHandle(PathElement.groupElement("cStencilBits"));
	    public PIXELFORMATDESCRIPTOR cStencilBits(int value) {cStencilBits.set(mem, 0, (byte)value); return(this);}
	    private static final VarHandle cAuxBuffers = _PIXELFORMATDESCRIPTOR.varHandle(PathElement.groupElement("cAuxBuffers"));
	    public PIXELFORMATDESCRIPTOR cAuxBuffers(int value) {cAuxBuffers.set(mem, 0, (byte)value); return(this);}
	    private static final VarHandle iLayerType = _PIXELFORMATDESCRIPTOR.varHandle(PathElement.groupElement("iLayerType"));
	    public PIXELFORMATDESCRIPTOR iLayerType(int value) {iLayerType.set(mem, 0, (byte)value); return(this);}
	}

	public PIXELFORMATDESCRIPTOR PIXELFORMATDESCRIPTOR() {
	    return(new PIXELFORMATDESCRIPTOR());
	}

	private final MethodHandle ChoosePixelFormat = ld.downcallHandle(gdi32.find("ChoosePixelFormat").get(), FunctionDescriptor.of(C_INT, HDC, ADDRESS));
	public int ChoosePixelFormat(Handle hDC, Win32.PIXELFORMATDESCRIPTOR ppfd) {
	    int rv;
	    try {
		rv = (int)ChoosePixelFormat.invoke(hDC.bits, ppfd.mem());
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv == 0)
		throw(lasterror());
	    return(rv);
	}

	private final MethodHandle SetPixelFormat = ld.downcallHandle(gdi32.find("SetPixelFormat").get(), FunctionDescriptor.of(BOOL, HDC, C_INT, ADDRESS));
	public void SetPixelFormat(Handle hDC, int format, Win32.PIXELFORMATDESCRIPTOR ppfd) {
	    int rv;
	    try {
		rv = (int)SetPixelFormat.invoke(hDC.bits, format, ppfd.mem());
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv == 0)
		throw(lasterror());
	}

	private final MethodHandle DescribePixelFormat = ld.downcallHandle(gdi32.find("DescribePixelFormat").get(), FunctionDescriptor.of(C_INT, HDC, C_INT, UINT, ADDRESS));
	public int DescribePixelFormat(Handle hDC, int iPixelFormat, Win32.PIXELFORMATDESCRIPTOR ppfd) {
	    int rv;
	    try {
		rv = (int)DescribePixelFormat.invoke(hDC.bits, iPixelFormat, (int)ppfd.mem().byteSize(), ppfd.mem());
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv == 0)
		throw(lasterror());
	    return(rv);
	}

	private final MethodHandle SwapBuffers = ld.downcallHandle(gdi32.find("SwapBuffers").get(), FunctionDescriptor.of(BOOL, HDC));
	public void SwapBuffers(Handle hDC) {
	    int rv;
	    try {
		rv = (int)SwapBuffers.invoke(hDC.bits);
	    } catch(Throwable e) {
		throw(new InvocationException(e));
	    }
	    if(rv == 0)
		throw(lasterror());
	}
    }

    private static Win32 instance = null;
    public static Win32 get() {
	if(instance == null) {
	    synchronized(Win32.class) {
		if(instance == null)
		    instance = tryload("Win32", Win64Unicode::new);
	    }
	}
	return(instance);
    }
}
