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

package haven.ffi.steam;

import haven.*;
import haven.ffi.*;
import java.nio.*;
import java.lang.invoke.*;
import java.lang.foreign.*;
import java.lang.foreign.MemoryLayout.PathElement;
import static haven.ffi.ABI.*;
import static haven.ffi.FUtils.*;
import static java.lang.foreign.ValueLayout.ADDRESS;

public abstract class SteamApi {
    public static final int k_EResultNone = 0;
    public static final int k_EResultOK	= 1;
    public static final int k_EResultFail = 2;
    public static final int k_EResultNoConnection = 3;
    public static final int k_EResultNoConnectionRetry = 4;
    public static final int k_EResultInvalidPassword = 5;
    public static final int k_EResultLoggedInElsewhere = 6;
    public static final int k_EResultInvalidProtocolVer = 7;
    public static final int k_EResultInvalidParam = 8;
    public static final int k_EResultFileNotFound = 9;
    public static final int k_EResultBusy = 10;
    public static final int k_EResultInvalidState = 11;
    public static final int k_EResultInvalidName = 12;
    public static final int k_EResultInvalidEmail = 13;
    public static final int k_EResultDuplicateName = 14;
    public static final int k_EResultAccessDenied = 15;
    public static final int k_EResultTimeout = 16;
    public static final int k_EResultBanned = 17;
    public static final int k_EResultAccountNotFound = 18;
    public static final int k_EResultInvalidSteamID = 19;
    public static final int k_EResultServiceUnavailable = 20;
    public static final int k_EResultNotLoggedOn = 21;
    public static final int k_EResultPending = 22;
    public static final int k_EResultEncryptionFailure = 23;
    public static final int k_EResultInsufficientPrivilege = 24;
    public static final int k_EResultLimitExceeded = 25;
    public static final int k_EResultRevoked = 26;
    public static final int k_EResultExpired = 27;
    public static final int k_EResultAlreadyRedeemed = 28;
    public static final int k_EResultDuplicateRequest = 29;
    public static final int k_EResultAlreadyOwned = 30;
    public static final int k_EResultIPNotFound = 31;
    public static final int k_EResultPersistFailed = 32;
    public static final int k_EResultLockingFailed = 33;
    public static final int k_EResultLogonSessionReplaced = 34;
    public static final int k_EResultConnectFailed = 35;
    public static final int k_EResultHandshakeFailed = 36;
    public static final int k_EResultIOFailure = 37;
    public static final int k_EResultRemoteDisconnect = 38;
    public static final int k_EResultShoppingCartNotFound = 39;
    public static final int k_EResultBlocked = 40;
    public static final int k_EResultIgnored = 41;
    public static final int k_EResultNoMatch = 42;
    public static final int k_EResultAccountDisabled = 43;
    public static final int k_EResultServiceReadOnly = 44;
    public static final int k_EResultAccountNotFeatured = 45;
    public static final int k_EResultAdministratorOK = 46;
    public static final int k_EResultContentVersion = 47;
    public static final int k_EResultTryAnotherCM = 48;
    public static final int k_EResultPasswordRequiredToKickSession = 49;
    public static final int k_EResultAlreadyLoggedInElsewhere = 50;
    public static final int k_EResultSuspended = 51;
    public static final int k_EResultCancelled = 52;
    public static final int k_EResultDataCorruption = 53;
    public static final int k_EResultDiskFull = 54;
    public static final int k_EResultRemoteCallFailed = 55;
    public static final int k_EResultPasswordUnset = 56;
    public static final int k_EResultExternalAccountUnlinked = 57;
    public static final int k_EResultPSNTicketInvalid = 58;
    public static final int k_EResultExternalAccountAlreadyLinked = 59;
    public static final int k_EResultRemoteFileConflict = 60;
    public static final int k_EResultIllegalPassword = 61;
    public static final int k_EResultSameAsPreviousValue = 62;
    public static final int k_EResultAccountLogonDenied = 63;
    public static final int k_EResultCannotUseOldPassword = 64;
    public static final int k_EResultInvalidLoginAuthCode = 65;
    public static final int k_EResultAccountLogonDeniedNoMail = 66;
    public static final int k_EResultHardwareNotCapableOfIPT = 67;
    public static final int k_EResultIPTInitError = 68;
    public static final int k_EResultParentalControlRestricted = 69;
    public static final int k_EResultFacebookQueryError = 70;
    public static final int k_EResultExpiredLoginAuthCode = 71;
    public static final int k_EResultIPLoginRestrictionFailed = 72;
    public static final int k_EResultAccountLockedDown = 73;
    public static final int k_EResultAccountLogonDeniedVerifiedEmailRequired = 74;
    public static final int k_EResultNoMatchingURL = 75;
    public static final int k_EResultBadResponse = 76;
    public static final int k_EResultRequirePasswordReEntry = 77;
    public static final int k_EResultValueOutOfRange = 78;
    public static final int k_EResultUnexpectedError = 79;
    public static final int k_EResultDisabled = 80;
    public static final int k_EResultInvalidCEGSubmission = 81;
    public static final int k_EResultRestrictedDevice = 82;
    public static final int k_EResultRegionLocked = 83;
    public static final int k_EResultRateLimitExceeded = 84;
    public static final int k_EResultAccountLoginDeniedNeedTwoFactor = 85;
    public static final int k_EResultItemDeleted = 86;
    public static final int k_EResultAccountLoginDeniedThrottle = 87;
    public static final int k_EResultTwoFactorCodeMismatch = 88;
    public static final int k_EResultTwoFactorActivationCodeMismatch = 89;
    public static final int k_EResultAccountAssociatedToMultiplePartners = 90;
    public static final int k_EResultNotModified = 91;
    public static final int k_EResultNoMobileDevice = 92;
    public static final int k_EResultTimeNotSynced = 93;
    public static final int k_EResultSmsCodeFailed = 94;
    public static final int k_EResultAccountLimitExceeded = 95;
    public static final int k_EResultAccountActivityLimitExceeded = 96;
    public static final int k_EResultPhoneActivityLimitExceeded = 97;
    public static final int k_EResultRefundToWallet = 98;
    public static final int k_EResultEmailSendFailure = 99;
    public static final int k_EResultNotSettled = 100;
    public static final int k_EResultNeedCaptcha = 101;
    public static final int k_EResultGSLTDenied = 102;
    public static final int k_EResultGSOwnerDenied = 103;
    public static final int k_EResultInvalidItemType = 104;
    public static final int k_EResultIPBanned = 105;
    public static final int k_EResultGSLTExpired = 106;
    public static final int k_EResultInsufficientFunds = 107;
    public static final int k_EResultTooManyPending = 108;
    public static final int k_EResultNoSiteLicensesFound = 109;
    public static final int k_EResultWGNetworkSendExceeded = 110;
    public static final int k_EResultAccountNotFriends = 111;
    public static final int k_EResultLimitedUserAccount = 112;
    public static final int k_EResultCantRemoveItem = 113;
    public static final int k_EResultAccountDeleted = 114;
    public static final int k_EResultExistingUserCancelledLicense = 115;
    public static final int k_EResultCommunityCooldown = 116;
    public static final int k_EResultNoLauncherSpecified = 117;
    public static final int k_EResultMustAgreeToSSA = 118;
    public static final int k_EResultLauncherMigrated = 119;
    public static final int k_EResultSteamRealmMismatch = 120;
    public static final int k_EResultInvalidSignature = 121;
    public static final int k_EResultParseFailure = 122;
    public static final int k_EResultNoVerifiedPhone = 123;
    public static final int k_EResultInsufficientBattery = 124;
    public static final int k_EResultChargerRequired = 125;
    public static final int k_EResultCachedCredentialInvalid = 126;
    public static final int K_EResultPhoneNumberIsVOIP = 127;

    public static final int k_EPositionInvalid = -1;
    public static final int k_EPositionTopLeft = 0;
    public static final int k_EPositionTopRight = 1;
    public static final int k_EPositionBottomLeft = 2;
    public static final int k_EPositionBottomRight = 3;

    public static final int k_EActivateGameOverlayToWebPageMode_Default = 0;
    public static final int k_EActivateGameOverlayToWebPageMode_Modal = 1;

    public static final int k_iSteamUserCallbacks    = 100;
    public static final int k_iSteamFriendsCallbacks = 300;
    public static final int k_iSteamUtilsCallbacks   = 700;

    public abstract boolean Init();
    public abstract boolean IsSteamRunning();
    public abstract HSteamPipe GetHSteamPipe();

    public abstract void ManualDispatch_Init();
    public abstract void ManualDispatch_RunFrame(HSteamPipe pipe);
    public abstract CallbackMsg ManualDispatch_GetNextCallback(HSteamPipe pipe);
    public abstract void ManualDispatch_FreeLastCallback(HSteamPipe pipe);

    public abstract SteamUtils SteamUtils();
    public abstract SteamUser SteamUser();
    public abstract SteamFriends SteamFriends();

    public static interface HSteamPipe {
    }

    public static class CallbackMsg {
	public final int id;
	public final MemorySegment data;

	public CallbackMsg(int id, MemorySegment data) {
	    this.id = id;
	    this.data = data;
	}
    }

    public static interface SteamUtils {
	public int GetAppID();
	public void SetOverlayNotificationPosition(int position);
    }

    public static interface SteamUser {
	public long GetSteamID();
	public int GetAuthTicketForWebApi(String identity);
    }

    public static interface SteamFriends {
	public String GetPersonaName();
	public boolean SetRichPresence(String key, String value);
    }

    public static interface GetTicketForWebApiResponse {
	public static final int k_iCallback = k_iSteamUserCallbacks + 68;

	public int AuthTicket();
	public int Result();
	public byte[] Ticket();
    }
    public abstract GetTicketForWebApiResponse GetTicketForWebApiResponse(CallbackMsg callback);

    public static abstract class Base extends SteamApi {
	public static final MemoryLayout UINT8 = ValueLayout.JAVA_BYTE;
	public static final MemoryLayout INT32 = ValueLayout.JAVA_INT;
	public static final MemoryLayout UINT32 = ValueLayout.JAVA_INT;
	public static final MemoryLayout UINT64 = ValueLayout.JAVA_LONG;
	public static final MemoryLayout UINT64_STEAMID = UINT64;
	public static final MemoryLayout HSteamPipe = UINT32;
	public static final MemoryLayout HSteamUser = UINT32;
	public static final MemoryLayout HAuthTicket = UINT32;
	public static final MemoryLayout EResult = C_ENUM;

	protected abstract MethodHandle lookup(String name, FunctionDescriptor sig, Linker.Option... options);

	static class HSteamPipe implements SteamApi.HSteamPipe {
	    final int handle;

	    HSteamPipe(int handle) {
		this.handle = handle;
	    }
	}

	private final MethodHandle SteamAPI_Init = lookup("SteamAPI_Init", FunctionDescriptor.of(C_BOOL));
	public boolean Init() {
	    try {
		return((int)SteamAPI_Init.invoke() != 0);
	    } catch(Throwable e) {throw(new RuntimeException(e));}
	}

	private final MethodHandle SteamAPI_IsSteamRunning = lookup("SteamAPI_IsSteamRunning", FunctionDescriptor.of(C_BOOL));
	public boolean IsSteamRunning() {
	    try {
		return((int)SteamAPI_IsSteamRunning.invoke() != 0);
	    } catch(Throwable e) {throw(new RuntimeException(e));}
	}

	private final MethodHandle SteamAPI_GetHSteamPipe = lookup("SteamAPI_GetHSteamPipe", FunctionDescriptor.of(HSteamPipe));
	public HSteamPipe GetHSteamPipe() {
	    int rv;
	    try {
		rv = (int)SteamAPI_GetHSteamPipe.invoke();
	    } catch(Throwable e) {throw(new RuntimeException(e));}
	    return(new HSteamPipe(rv));
	}

	private final MethodHandle SteamAPI_ManualDispatch_Init = lookup("SteamAPI_ManualDispatch_Init", FunctionDescriptor.ofVoid());
	public void ManualDispatch_Init() {
	    try {
		SteamAPI_ManualDispatch_Init.invoke();
	    } catch(Throwable e) {throw(new RuntimeException(e));}
	}

	private final MethodHandle SteamAPI_ManualDispatch_RunFrame = lookup("SteamAPI_ManualDispatch_RunFrame", FunctionDescriptor.ofVoid(HSteamPipe));
	public void ManualDispatch_RunFrame(SteamApi.HSteamPipe pipe) {
	    try {
		SteamAPI_ManualDispatch_RunFrame.invoke(((HSteamPipe)pipe).handle);
	    } catch(Throwable e) {throw(new RuntimeException(e));}
	}

	public static final StructLayout _CallbackMsg_t = struct(new MemoryLayout[] {
	    HSteamUser.withName("m_hSteamUser"),
	    C_INT.withName("m_iCallback"),
	    ADDRESS.withName("m_pubParam"),
	    C_INT.withName("m_cubParam"),
	});
	private static final VarHandle _CallbackMsg_hSteamUser = _CallbackMsg_t.varHandle(PathElement.groupElement("m_hSteamUser"));
	private static final VarHandle _CallbackMsg_iCallback = _CallbackMsg_t.varHandle(PathElement.groupElement("m_iCallback"));
	private static final VarHandle _CallbackMsg_pubParam = _CallbackMsg_t.varHandle(PathElement.groupElement("m_pubParam"));
	private static final VarHandle _CallbackMsg_cubParam = _CallbackMsg_t.varHandle(PathElement.groupElement("m_cubParam"));

	private final MethodHandle SteamAPI_ManualDispatch_GetNextCallback = lookup("SteamAPI_ManualDispatch_GetNextCallback", FunctionDescriptor.of(C_BOOL, HSteamPipe, ADDRESS));
	public CallbackMsg ManualDispatch_GetNextCallback(SteamApi.HSteamPipe pipe) {
	    try(Arena st = Arena.ofConfined()) {
		int rv;
		MemorySegment buf = st.allocate(_CallbackMsg_t);
		try {
		    rv = (int)SteamAPI_ManualDispatch_GetNextCallback.invoke(((HSteamPipe)pipe).handle, buf);
		} catch(Throwable e) {throw(new RuntimeException(e));}
		if(rv == 0)
		    return(null);
		MemorySegment data = (MemorySegment)_CallbackMsg_pubParam.get(buf, 0);
		int sz = (int)_CallbackMsg_cubParam.get(buf, 0);
		return(new CallbackMsg((int)_CallbackMsg_iCallback.get(buf, 0),
				       nullp(data) ? null : memcpya(data.reinterpret(sz), 0, sz)));
	    }
	}

	private final MethodHandle SteamAPI_ManualDispatch_FreeLastCallback = lookup("SteamAPI_ManualDispatch_FreeLastCallback", FunctionDescriptor.ofVoid(HSteamPipe));
	public void ManualDispatch_FreeLastCallback(SteamApi.HSteamPipe pipe) {
	    try {
		SteamAPI_ManualDispatch_FreeLastCallback.invoke(((HSteamPipe)pipe).handle);
	    } catch(Throwable e) {throw(new RuntimeException(e));}
	}

	public class SteamUtils implements SteamApi.SteamUtils {
	    final MemorySegment self;

	    private SteamUtils(MemorySegment self) {
		this.self = self;
	    }

	    private final MethodHandle SteamAPI_ISteamUtils_GetAppID = lookup("SteamAPI_ISteamUtils_GetAppID", FunctionDescriptor.of(INT32, ADDRESS));
	    public int GetAppID() {
		try {
		    return((int)SteamAPI_ISteamUtils_GetAppID.invoke(self));
		} catch(Throwable e) {throw(new RuntimeException(e));}
	    }

	    private final MethodHandle SteamAPI_ISteamUtils_SetOverlayNotificationPosition = lookup("SteamAPI_ISteamUtils_SetOverlayNotificationPosition", FunctionDescriptor.ofVoid(C_ENUM));
	    public void SetOverlayNotificationPosition(int position) {
		try {
		    SteamAPI_ISteamUtils_SetOverlayNotificationPosition.invoke(position);
		} catch(Throwable e) {throw(new RuntimeException(e));}
	    }
	}

	private final MethodHandle SteamAPI_SteamUtils_v010 = lookup("SteamAPI_SteamUtils_v010", FunctionDescriptor.of(ADDRESS));
	private SteamUtils SteamUtils = null;
	public SteamUtils SteamUtils() {
	    if(SteamUtils != null)
		return(SteamUtils);
	    MemorySegment self;
	    try {
		self = (MemorySegment)SteamAPI_SteamUtils_v010.invoke();
	    } catch(Throwable e) {throw(new RuntimeException(e));}
	    if(nullp(self))
		return(null);
	    return(SteamUtils = new SteamUtils(self));
	}

	public class SteamUser implements SteamApi.SteamUser {
	    final MemorySegment self;

	    private SteamUser(MemorySegment self) {
		this.self = self;
	    }

	    private final MethodHandle SteamAPI_ISteamUser_GetSteamID = lookup("SteamAPI_ISteamUser_GetSteamID", FunctionDescriptor.of(UINT64_STEAMID, ADDRESS));
	    public long GetSteamID() {
		try {
		    return((long)SteamAPI_ISteamUser_GetSteamID.invoke(self));
		} catch(Throwable e) {throw(new RuntimeException(e));}
	    }

	    private final MethodHandle SteamAPI_ISteamUser_GetAuthTicketForWebApi = lookup("SteamAPI_ISteamUser_GetAuthTicketForWebApi", FunctionDescriptor.of(HAuthTicket, ADDRESS, ADDRESS));
	    public int GetAuthTicketForWebApi(String identity) {
		try(Arena st = Arena.ofConfined()) {
		    return((int)SteamAPI_ISteamUser_GetAuthTicketForWebApi.invoke(self, identity == null ? MemorySegment.NULL : st.allocateFrom(identity, Utils.utf8)));
		} catch(Throwable e) {throw(new RuntimeException(e));}
	    }

	    private final MethodHandle SteamAPI_ISteamUser_CancelAuthTicket = lookup("SteamAPI_ISteamUser_CancelAuthTicket", FunctionDescriptor.ofVoid(ADDRESS, HAuthTicket));
	    public void CancelAuthTicket(int ticket) {
		try {
		    SteamAPI_ISteamUser_CancelAuthTicket.invoke(self, ticket);
		} catch(Throwable e) {throw(new RuntimeException(e));}
	    }
	}

	private final MethodHandle SteamAPI_SteamUser_v023 = lookup("SteamAPI_SteamUser_v023", FunctionDescriptor.of(ADDRESS));
	private SteamUser SteamUser = null;
	public SteamUser SteamUser() {
	    if(SteamUser != null)
		return(SteamUser);
	    MemorySegment self;
	    try {
		self = (MemorySegment)SteamAPI_SteamUser_v023.invoke();
	    } catch(Throwable e) {throw(new RuntimeException(e));}
	    if(nullp(self))
		return(null);
	    return(SteamUser = new SteamUser(self));
	}

	public class SteamFriends implements SteamApi.SteamFriends {
	    final MemorySegment self;

	    private SteamFriends(MemorySegment self) {
		this.self = self;
	    }

	    private final MethodHandle SteamAPI_ISteamFriends_GetPersonaName = lookup("SteamAPI_ISteamFriends_GetPersonaName", FunctionDescriptor.of(ADDRESS, ADDRESS));
	    public String GetPersonaName() {
		MemorySegment rv;
		try {
		    rv = (MemorySegment)SteamAPI_ISteamFriends_GetPersonaName.invoke(self);
		} catch(Throwable e) {throw(new RuntimeException(e));}
		return(rv.reinterpret(Long.MAX_VALUE).getString(0, Utils.utf8));
	    }

	    private final MethodHandle SteamAPI_ISteamFriends_SetRichPresence = lookup("SteamAPI_ISteamFriends_SetRichPresence", FunctionDescriptor.of(C_BOOL, ADDRESS, ADDRESS, ADDRESS));
	    public boolean SetRichPresence(String key, String value) {
		try(Arena st = Arena.ofConfined()) {
		    int rv;
		    try {
			rv = (int)SteamAPI_ISteamFriends_SetRichPresence.invoke(self, st.allocateFrom(key, Utils.utf8), st.allocateFrom(value, Utils.utf8));
		    } catch(Throwable e) {throw(new RuntimeException(e));}
		    return(rv != 0);
		}
	    }

	    private final MethodHandle SteamAPI_ISteamFriends_ActivateGameOverlayToWebPage = lookup("SteamAPI_ISteamFriends_ActivateGameOverlayToWebPage", FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, C_ENUM));
	    public void ActivateGameOverlayToWebPage(String url, int mode) {
		try(Arena st = Arena.ofConfined()) {
		    try {
			SteamAPI_ISteamFriends_ActivateGameOverlayToWebPage.invoke(self, st.allocateFrom(url, Utils.utf8), mode);
		    } catch(Throwable e) {throw(new RuntimeException(e));}
		}
	    }
	}

	private final MethodHandle SteamAPI_SteamFriends_v017 = lookup("SteamAPI_SteamFriends_v017", FunctionDescriptor.of(ADDRESS));
	private SteamFriends SteamFriends = null;
	public SteamFriends SteamFriends() {
	    if(SteamFriends != null)
		return(SteamFriends);
	    MemorySegment self;
	    try {
		self = (MemorySegment)SteamAPI_SteamFriends_v017.invoke();
	    } catch(Throwable e) {throw(new RuntimeException(e));}
	    if(nullp(self))
		return(null);
	    return(SteamFriends = new SteamFriends(self));
	}

	public static final StructLayout _GetTicketForWebApiResult_t = struct(new MemoryLayout[] {
	    HAuthTicket.withName("m_hAuthTicket"),
	    EResult.withName("m_eResult"),
	    C_INT.withName("m_cubTicket"),
	    MemoryLayout.sequenceLayout(2560, UINT8).withName("m_rgubTicket"),
	});
	static class GetTicketForWebApiResponse implements SteamApi.GetTicketForWebApiResponse {
	    private static final VarHandle m_hAuthTicket = _GetTicketForWebApiResult_t.varHandle(PathElement.groupElement("m_hAuthTicket"));
	    private static final VarHandle m_eResult = _GetTicketForWebApiResult_t.varHandle(PathElement.groupElement("m_eResult"));
	    private static final VarHandle m_cubTicket = _GetTicketForWebApiResult_t.varHandle(PathElement.groupElement("m_cubTicket"));
	    private static final long m_rgubTicket = _GetTicketForWebApiResult_t.byteOffset(PathElement.groupElement("m_rgubTicket"));
	    final MemorySegment mem;

	    GetTicketForWebApiResponse(MemorySegment mem) {
		this.mem = mem;
	    }

	    public int AuthTicket() {return((int)m_hAuthTicket.get(mem, 0));}
	    public int Result() {return((int)m_eResult.get(mem, 0));}
	    public byte[] Ticket() {return(memcpy(mem, m_rgubTicket, (int)m_cubTicket.get(mem, 0)));}
	}

	public GetTicketForWebApiResponse GetTicketForWebApiResponse(CallbackMsg callback) {
	    return(new GetTicketForWebApiResponse(callback.data));
	}
    }

    public static class libsteam_api_so extends Base {
	private SymbolLookup lib = null;
	protected MethodHandle lookup(String name, FunctionDescriptor sig, Linker.Option... options) {
	    if(lib == null)
		lib = loadlib("libsteam_api.so", Arena.global());
	    MemorySegment addr = lib.find(name).get();
	    if(nullp(addr))
		throw(new MissingFunction("name"));
	    return(ld.downcallHandle(addr, sig, options));
	}
    }

    private static SteamApi instance = null;
    public static SteamApi get() {
	if(instance == null) {
	    synchronized(SteamApi.class) {
		if(instance == null) {
		    instance = new libsteam_api_so();
		    instance.Init();
		}
	    }
	}
	return(instance);
    }

    public static void main(String[] args) throws Exception {
	SteamApi api = get();
	haven.Debug.dump(api.SteamUtils().GetAppID());
	api.ManualDispatch_Init();
	haven.Debug.dump(Long.toUnsignedString(api.SteamUser().GetSteamID(), 16), api.SteamFriends().GetPersonaName());
	api.SteamUser().GetAuthTicketForWebApi(null);
	HSteamPipe pipe = api.GetHSteamPipe();
	for(double st = Utils.rtime(); Utils.rtime() < (st + 5); Thread.sleep(100)) {
	    api.ManualDispatch_RunFrame(pipe);
	    CallbackMsg cb = api.ManualDispatch_GetNextCallback(pipe);
	    if(cb != null) {
		if(cb.id == GetTicketForWebApiResponse.k_iCallback) {
		    Debug.dump(api.GetTicketForWebApiResponse(cb).Ticket());
		} else {
		    Debug.dump(cb.id, cb.data);
		}
		api.ManualDispatch_FreeLastCallback(pipe);
	    }
	}
    }
}
