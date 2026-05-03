import permissionMap from "../config/permission-map";

function PermissionGate({ permissions = [], anyOf = [], children, fallback = null }) {
  const userCodes = permissions || [];
  const required = anyOf.map((item) => permissionMap[item] || item);
  if (required.length === 0) {
    return children;
  }
  const ok = required.some((code) => userCodes.includes(code));
  return ok ? children : fallback;
}

export default PermissionGate;

