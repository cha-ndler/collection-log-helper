/*
 * Copyright (c) 2025, cha-ndler
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.collectionloghelper.sync;

/**
 * Outcome of a {@link CollectionLogNetImporter#importProfile} call.
 * Immutable value type — use the static factory methods.
 */
public final class ImportResult
{
	public enum Status
	{
		/** Import completed successfully. */
		SUCCESS,
		/** The requested username was not found on collectionlog.net (HTTP 404). */
		USER_NOT_FOUND,
		/** The service returned a non-404 error or a network error occurred. */
		SERVICE_UNAVAILABLE,
		/** The response body could not be parsed as expected JSON. */
		MALFORMED_RESPONSE
	}

	private final Status status;
	private final int itemsMarked;
	private final String username;
	private final int httpCode;

	private ImportResult(Status status, int itemsMarked, String username, int httpCode)
	{
		this.status = status;
		this.itemsMarked = itemsMarked;
		this.username = username;
		this.httpCode = httpCode;
	}

	public static ImportResult success(int itemsMarked)
	{
		return new ImportResult(Status.SUCCESS, itemsMarked, null, 200);
	}

	public static ImportResult userNotFound(String username)
	{
		return new ImportResult(Status.USER_NOT_FOUND, 0, username, 404);
	}

	public static ImportResult serviceUnavailable(int httpCode)
	{
		return new ImportResult(Status.SERVICE_UNAVAILABLE, 0, null, httpCode);
	}

	public static ImportResult malformedResponse()
	{
		return new ImportResult(Status.MALFORMED_RESPONSE, 0, null, 0);
	}

	public Status getStatus()
	{
		return status;
	}

	public boolean isSuccess()
	{
		return status == Status.SUCCESS;
	}

	public int getItemsMarked()
	{
		return itemsMarked;
	}

	/** Username associated with a {@link Status#USER_NOT_FOUND} result; {@code null} otherwise. */
	public String getUsername()
	{
		return username;
	}

	public int getHttpCode()
	{
		return httpCode;
	}

	/** Human-readable toast message suitable for display in the panel. */
	public String toToastMessage()
	{
		switch (status)
		{
			case SUCCESS:
				return "Synced " + itemsMarked + " item" + (itemsMarked == 1 ? "" : "s")
					+ " from collectionlog.net";
			case USER_NOT_FOUND:
				return "collectionlog.net: user not found";
			case SERVICE_UNAVAILABLE:
				return "collectionlog.net: service unavailable";
			case MALFORMED_RESPONSE:
				return "collectionlog.net: unexpected response format";
			default:
				return "collectionlog.net: unknown error";
		}
	}

	@Override
	public String toString()
	{
		return "ImportResult{status=" + status
			+ ", itemsMarked=" + itemsMarked
			+ ", httpCode=" + httpCode + "}";
	}
}
