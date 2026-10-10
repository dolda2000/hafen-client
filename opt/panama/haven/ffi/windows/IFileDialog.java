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

import java.lang.invoke.*;
import java.lang.foreign.*;
import haven.ffi.*;
import haven.ffi.windows.Win32.*;
import haven.ffi.windows.COM.*;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static haven.ffi.ABI.*;

public interface IFileDialog extends IModalWindow {
    public static final IID<IFileDialog> IID = new IID<>(IFileDialog.class, 0x42f85136_db7e_439cl, 0x85f1_e4075d135fc8l);
    public static final CLSID CLSID_FileOpenDialog = new CLSID(0xdc1c5a9c_e88a_4ddel, 0xa5a1_60f82a20aef7l);
    public static final CLSID CLSID_FileSaveDialog = new CLSID(0xc0b4e2f3_ba21_4773l, 0x8dba_335ec946eb8bl);
    static final Win32 win = Win32.get();
    static final COM com = COM.get();

    public static final Descriptor sig_SetFileTypes = new Descriptor(4, FunctionDescriptor.of(com.HRESULT(), ADDRESS, com.UINT(), ADDRESS)).upcall(UpcallAdapter.nil);
    public void SetFileTypes(String[][] filterspec);
    public static void downcall_SetFileTypes(COM.Downcall inf, MemorySegment self, String[][] filterspec) {
	try(Arena st = Arena.ofConfined()) {
	    MemorySegment buf = st.allocate(ADDRESS, 2 * filterspec.length);
	    for(int i = 0; i < filterspec.length; i++) {
		buf.setAtIndex(ADDRESS, (i * 2) + 0, st.allocateFrom(filterspec[i][0], win.C_WCHARSET()));
		buf.setAtIndex(ADDRESS, (i * 2) + 1, st.allocateFrom(filterspec[i][1], win.C_WCHARSET()));
	    }
	    int rv;
	    try {
		rv = (int)inf.fun.invoke(inf.com.getvtab(self, sig_SetFileTypes.vindex), self, filterspec.length, buf);
	    } catch(Throwable t) {
		throw(new InvocationException(t));
	    }
	    if(!inf.com.SUCCEEDED(rv))
		throw(new HResultError(rv));
	}
    }

    public static final Descriptor sig_SetFileTypeIndex = new Descriptor(5, FunctionDescriptor.of(com.HRESULT(), ADDRESS, com.UINT()));
    public void SetFileTypeIndex(int filetype);
    public static final Descriptor sig_GetFileTypeIndex = new Descriptor(6, FunctionDescriptor.of(com.HRESULT(), ADDRESS, ADDRESS), com.UINT());
    public int GetFileTypeIndex();
    public static final Descriptor sig_Advise = new Descriptor(7, FunctionDescriptor.of(com.HRESULT(), ADDRESS, ADDRESS, ADDRESS), com.DWORD());
    public int Advise(IFileDialogEvents callback);
    public static final Descriptor sig_Unadvise = new Descriptor(8, FunctionDescriptor.of(com.HRESULT(), ADDRESS, com.DWORD()));
    public void Unadvise(int cookie);
    public static final Descriptor sig_SetOptions = new Descriptor(9, FunctionDescriptor.of(com.HRESULT(), ADDRESS, C_ENUM));
    public void SetOptions(int options);
    public static final Descriptor sig_GetOptions = new Descriptor(10, FunctionDescriptor.of(com.HRESULT(), ADDRESS, ADDRESS), C_ENUM);
    public int GetOptions();
    public static final Descriptor sig_SetDefaultfolder = new Descriptor(11, FunctionDescriptor.of(com.HRESULT(), ADDRESS, ADDRESS));
    public void SetDefaultFolder(IShellItem item);
    public static final Descriptor sig_SetFolder = new Descriptor(12, FunctionDescriptor.of(com.HRESULT(), ADDRESS, ADDRESS));
    public void SetFolder(IShellItem item);
    public static final Descriptor sig_GetFolder = new Descriptor(13, FunctionDescriptor.of(com.HRESULT(), ADDRESS, ADDRESS), ADDRESS);
    public IShellItem GetFolder();
    public static final Descriptor sig_GetCurrentSelection = new Descriptor(14, FunctionDescriptor.of(com.HRESULT(), ADDRESS, ADDRESS), ADDRESS);
    public IShellItem GetCurrentSelection();
    public static final Descriptor sig_SetFileName = new Descriptor(15, FunctionDescriptor.of(com.HRESULT(), ADDRESS, ADDRESS));
    public void SetFileName(String name);
    public static final Descriptor sig_GetFileName = new Descriptor(16, FunctionDescriptor.of(com.HRESULT(), ADDRESS, ADDRESS), ADDRESS);
    public String GetFileName();
    public static final Descriptor sig_SetTitle = new Descriptor(17, FunctionDescriptor.of(com.HRESULT(), ADDRESS, ADDRESS));
    public void SetTitle(String title);
    public static final Descriptor sig_SetOKButtonLabel = new Descriptor(18, FunctionDescriptor.of(com.HRESULT(), ADDRESS, ADDRESS));
    public void SetOKButtonLabel(String text);
    public static final Descriptor sig_SetFileNameLabel = new Descriptor(19, FunctionDescriptor.of(com.HRESULT(), ADDRESS, ADDRESS));
    public void SetFileNameLabel(String text);
    public static final Descriptor sig_GetResult = new Descriptor(20, FunctionDescriptor.of(com.HRESULT(), ADDRESS, ADDRESS), ADDRESS);
    public IShellItem GetResult();
    public static final Descriptor sig_SetDefaultExtension = new Descriptor(22, FunctionDescriptor.of(com.HRESULT(), ADDRESS, ADDRESS));
    public void SetDefaultExtension(String text);
    public static final Descriptor sig_Close = new Descriptor(23, FunctionDescriptor.of(com.HRESULT(), ADDRESS, com.HRESULT()));
    public void Close(int hr);
}
