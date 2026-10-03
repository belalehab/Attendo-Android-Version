# Licensing Compatibility Verification
**Status**: VERIFIED against actual desktop code (src-tauri/src/security.rs).

## Desktop Implementation
- **Hardware Identifier Extraction**: The desktop app calls PowerShell via Rust (Get-CimInstance) to extract:
  1. Win32_BaseBoard.SerialNumber
  2. Win32_Processor.ProcessorId
  3. Win32_DiskDrive.SerialNumber
- **Identifier Processing**: These 3 values are concatenated as $board--. This string is hashed using SHA-256 and converted to a hex string. This hex string represents the hwId.
- **JWT Validation**: 
  - Algorithm: HS256
  - Secret Key: Attendo_Secure_RSA_2026_!@#_Key
  - Claims checked: hwId must match the current hardware hash, and exp must be valid.

## Android Adaptation
### 1. Unavoidable Platform Difference
An Android device does not have Win32_BaseBoard, Win32_Processor, or Win32_DiskDrive. It is impossible to generate the exact same source string for the hardware hash. Furthermore, users will run the Android app on a different physical device than the desktop app, requiring a distinct license to be issued for the mobile device.

### 2. Exact Compatibility
The JWT parsing, the secret key (Attendo_Secure_RSA_2026_!@#_Key), the HS256 algorithm, and the claim checks (hwId, exp) will be identical byte-for-byte.

### 3. Android Equivalent Hardware ID
On Android, we will extract Settings.Secure.ANDROID_ID. 
To match the expected formatting (a SHA-256 hex string), we will compute the SHA-256 hash of the ANDROID_ID and use that as the Android device's hwId. 
*Note*: The ANDROID_ID resets on factory wipe, which provides security properties analogous to the desktop's hardware binding.
