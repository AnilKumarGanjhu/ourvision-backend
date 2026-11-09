package com.ourvision.dto;

public class ApiResponse<T> {

	private int status;
	private String message;
	private T data;

	public ApiResponse() {
	}

	public ApiResponse(int status, String message, T data) {
		this.status = status;
		this.message = message;
		this.data = data;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}

	/**
	 * Success response with data.
	 */
	public static <T> ApiResponse<T> success(int status, String message, T data) {

		return new ApiResponse<>(status, message, data);
	}

	/**
	 * Success response with default status and message.
	 */
	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(200, "Success", data);
	}

	/**
	 * Success response with custom message and data.
	 */
	public static <T> ApiResponse<T> success(String message, T data) {

		return new ApiResponse<>(200, message, data);
	}

	/**
	 * Error response without data.
	 */
	public static ApiResponse<Void> error(int status, String message) {

		return new ApiResponse<>(status, message, null);
	}
}