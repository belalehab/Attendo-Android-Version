# QR Compatibility Test Vectors
**Status**: VERIFIED against actual desktop code (src/tauriApi.ts:467-486).

## Algorithm
1. Retrieve National ID.
2. Append SECRET_APP_KEY ("Attendo_Secure_2026_!@#").
3. Append ACADEMIC_YEAR ("2025-2026").
4. UTF-8 encode and perform SHA-256 hash.
5. Hex encode the hash.
6. Final Payload format: NationalID|Hash.

## Test Vectors

### Vector 1
*   **National ID**: 11111111111111
*   **Raw String**: 11111111111111Attendo_Secure_2026_!@#2025-2026
*   **Expected SHA-256 (Hex)**: 9dec2ace02575500e8c59d2b8ca926ecbf1d7bed3f6ccc9ba6ad7e841278ec51
*   **Expected Payload**: 11111111111111|9dec2ace02575500e8c59d2b8ca926ecbf1d7bed3f6ccc9ba6ad7e841278ec51

### Vector 2
*   **National ID**: 22222222222222
*   **Raw String**: 22222222222222Attendo_Secure_2026_!@#2025-2026
*   **Expected SHA-256 (Hex)**: 81590b9c1f79f805ab07ec2f550351590fed1c3ccfb49d33a94bc7ba23c83e22
*   **Expected Payload**: 22222222222222|81590b9c1f79f805ab07ec2f550351590fed1c3ccfb49d33a94bc7ba23c83e22

### Vector 3
*   **National ID**: 33333333333333
*   **Raw String**: 33333333333333Attendo_Secure_2026_!@#2025-2026
*   **Expected SHA-256 (Hex)**: ec4f20bfd3d84210264657e9fdf76d24a358d778afc0a4779b65bc9a3b0f8742
*   **Expected Payload**: 33333333333333|ec4f20bfd3d84210264657e9fdf76d24a358d778afc0a4779b65bc9a3b0f8742

## Implementation Note
The Android app MUST implement exactly this algorithm to generate QR codes (Zip bundle) or to validate scanned QR codes. The hashing prevents forged QR codes.
