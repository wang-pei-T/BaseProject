export function platformHasPerm(user, code) {
  if (!user || user.principalType !== "PLATFORM_ACCOUNT") return true;
  const list = user.platformPermissions;
  if (!list || !list.length) return false;
  return list.includes(code);
}

export function filterPlatformMenuItems(items, user) {
  return (items || [])
    .map((item) => {
      if (item.children) {
        const children = filterPlatformMenuItems(item.children, user);
        if (!children.length) return null;
        const { perm, ...rest } = item;
        return { ...rest, children };
      }
      if (item.perm) {
        const codes = Array.isArray(item.perm) ? item.perm : [item.perm];
        if (!codes.some((c) => platformHasPerm(user, c))) return null;
      }
      const { perm, ...rest } = item;
      return rest;
    })
    .filter(Boolean);
}
