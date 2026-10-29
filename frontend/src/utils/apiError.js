// Turns any failed API call into one simple shape that every form and toast can use:
//   { status, message, fieldErrors }   where fieldErrors looks like { email: "Email must be a valid address" }
export function parseApiError(error) {
  const data = error?.response?.data;

  if (data && typeof data === "object") {
    const fieldErrors = {};
    (data.fieldErrors ?? []).forEach(({ field, message }) => {
      if (!fieldErrors[field]) {
        fieldErrors[field] = message;
      }
    });
    return {
      status: error.response.status,
      message: data.message ?? "Something went wrong",
      fieldErrors,
    };
  }

  if (error?.request) {
    return {
      status: 0,
      message: "Cannot reach the server. Check your connection and try again.",
      fieldErrors: {},
    };
  }

  return { status: 0, message: error?.message ?? "Something went wrong", fieldErrors: {} };
}