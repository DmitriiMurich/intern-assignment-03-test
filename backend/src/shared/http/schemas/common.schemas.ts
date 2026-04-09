export const errorResponseSchema = {
  type: "object",
  additionalProperties: false,
  required: ["statusCode", "error", "message"],
  properties: {
    statusCode: { type: "integer", examples: [400] },
    error: { type: "string", examples: ["BAD_REQUEST"] },
    message: { type: "string", examples: ["Request validation failed"] },
  },
} as const;

export const healthResponseSchema = {
  type: "object",
  additionalProperties: false,
  required: ["status"],
  properties: {
    status: { type: "string", enum: ["ok"] },
  },
} as const;
