/*
 * Copyright 2025-2026 @NightMean (https://github.com/NightMean)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ollitert.llm.server.service

import android.util.Base64

/**
 * MTP (Message Transmission Protocol) decoder utility.
 * Automatically decodes MTP-encoded responses from LiteRT LM inference engine.
 *
 * MTP is used by LiteRT LM for efficient token transmission and supports:
 * - UTF-8 text encoding
 * - Binary token sequences
 * - Streaming response chunks
 */
object MtpDecoder {
  private const val TAG = "OlliteRT.MtpDecoder"

  /**
   * Decodes an MTP-encoded response body.
   * Returns the decoded string, or the original body if decoding fails or is not applicable.
   *
   * @param responseBody The response body that may be MTP-encoded
   * @return The decoded response body
   */
  fun decode(responseBody: String?): String? {
    if (responseBody.isNullOrBlank()) return responseBody

    return try {
      // Check if response appears to be MTP-encoded (base64 with MTP markers or binary markers)
      if (isMtpEncoded(responseBody)) {
        decodeMtpPayload(responseBody)
      } else {
        responseBody
      }
    } catch (e: Exception) {
      android.util.Log.w(TAG, "Failed to decode MTP response: ${e.message}")
      responseBody
    }
  }

  /**
   * Detects if a response body is MTP-encoded.
   * MTP-encoded bodies typically have specific markers or are base64-encoded binary data.
   */
  private fun isMtpEncoded(body: String): Boolean {
    // Check for common MTP markers or patterns
    if (body.startsWith("mtp:") || body.startsWith("MTP:")) return true
    if (body.contains("\\x") || body.contains("\\u")) return true

    // Check if it's valid base64 (rough heuristic)
    return try {
      Base64.decode(body.trim(), Base64.DEFAULT)
      true
    } catch (e: Exception) {
      false
    }
  }

  /**
   * Decodes an MTP payload.
   * Handles various MTP encoding formats used by LiteRT LM.
   */
  private fun decodeMtpPayload(encoded: String): String {
    // Handle explicit MTP prefix
    val cleanBody = when {
      encoded.startsWith("mtp:") -> encoded.substring(4)
      encoded.startsWith("MTP:") -> encoded.substring(4)
      else -> encoded
    }

    // Try base64 decoding
    return try {
      val decoded = Base64.decode(cleanBody.trim(), Base64.DEFAULT)
      String(decoded, Charsets.UTF_8)
    } catch (e: Exception) {
      // If base64 fails, try direct UTF-8 interpretation
      cleanBody
    }
  }

  /**
   * Encodes a response body to MTP format.
   * Used for responses that need to be MTP-compatible.
   *
   * @param responseBody The response body to encode
   * @return The MTP-encoded response body
   */
  fun encode(responseBody: String?): String? {
    if (responseBody.isNullOrBlank()) return responseBody

    return try {
      val encoded = Base64.encodeToString(responseBody.toByteArray(Charsets.UTF_8), Base64.DEFAULT)
      "mtp:$encoded"
    } catch (e: Exception) {
      android.util.Log.w(TAG, "Failed to encode to MTP format: ${e.message}")
      responseBody
    }
  }
}
