# -*- coding: utf-8 -*-
import os
import re
import shutil

BASE = os.path.join(os.path.dirname(__file__), "src", "main", "java", "com", "baseproject")
MODULES = os.path.join(BASE, "modules")


def pkg_mod(mod):
    return {"audit": "tenantaudit", "log": "tenantlog"}.get(mod, mod)


def infer_target(rel):
    rel = rel.replace("\\", "/")
    parts = rel.split("/")
    name = parts[-1]
    if parts[0] == "platform":
        if parts[1] == "entity":
            return (
                os.path.join("domain", "system", "platform", name),
                "com.baseproject.domain.system.platform",
            )
        if parts[1] == "mapper":
            return (
                os.path.join("mapper", "system", "platform", name),
                "com.baseproject.mapper.system.platform",
            )
        sub = parts[1]
        if name.endswith("Controller.java"):
            return (
                os.path.join("controller", "system", "platform", sub, name),
                "com.baseproject.controller.system.platform." + sub,
            )
        return (
            os.path.join("service", "system", "platform", sub, name),
            "com.baseproject.service.system.platform." + sub,
        )
    mod = parts[0]
    pm = pkg_mod(mod)
    kind = parts[1]
    if kind == "controller":
        return (
            os.path.join("controller", "system", pm, name),
            "com.baseproject.controller.system." + pm,
        )
    if kind == "service":
        return (
            os.path.join("service", "system", pm, name),
            "com.baseproject.service.system." + pm,
        )
    if kind == "entity":
        return (
            os.path.join("domain", "system", pm, name),
            "com.baseproject.domain.system." + pm,
        )
    if kind == "mapper":
        return (
            os.path.join("mapper", "system", pm, name),
            "com.baseproject.mapper.system." + pm,
        )
    if kind == "domain":
        return (
            os.path.join("domain", "system", pm, name),
            "com.baseproject.domain.system." + pm,
        )
    if kind == "dto":
        return (
            os.path.join("service", "system", pm, "dto", name),
            "com.baseproject.service.system." + pm + ".dto",
        )
    raise RuntimeError("unknown layout: " + rel)


def main():
    class_map = {}
    moves = []
    for root, _, files in os.walk(MODULES):
        for f in files:
            if not f.endswith(".java"):
                continue
            full = os.path.join(root, f)
            rel = os.path.relpath(full, MODULES)
            new_rel, new_pkg = infer_target(rel)
            dest = os.path.join(BASE, new_rel)
            with open(full, "r", encoding="utf-8") as fh:
                text = fh.read()
            cm = re.search(r"public\s+(?:class|interface|enum)\s+(\w+)", text)
            if not cm:
                raise RuntimeError("no class: " + full)
            cname = cm.group(1)
            m = re.search(r"^package\s+([\w.]+);", text, re.M)
            if not m:
                raise RuntimeError("no package: " + full)
            old_pkg = m.group(1)
            old_fqn = old_pkg + "." + cname
            new_fqn = new_pkg + "." + cname
            class_map[old_fqn] = new_fqn
            moves.append((dest, text, new_pkg))

    for dest, text, new_pkg in moves:
        os.makedirs(os.path.dirname(dest), exist_ok=True)
        text2 = re.sub(r"^package\s+[\w.]+;", "package " + new_pkg + ";", text, count=1, flags=re.M)
        with open(dest, "w", encoding="utf-8", newline="\n") as out:
            out.write(text2)

    out_map = os.path.join(os.path.dirname(__file__), "system_package_class_map.tsv")
    with open(out_map, "w", encoding="utf-8") as mf:
        for k in sorted(class_map.keys()):
            mf.write(k + "\t" + class_map[k] + "\n")

    shutil.rmtree(MODULES)

    for root, _, files in os.walk(BASE):
        for f in files:
            if not f.endswith(".java"):
                continue
            path = os.path.join(root, f)
            with open(path, "r", encoding="utf-8") as fh:
                text = fh.read()
            orig = text
            for old_fqn, new_fqn in sorted(class_map.items(), key=lambda x: -len(x[0])):
                text = text.replace(old_fqn, new_fqn)
            if text != orig:
                with open(path, "w", encoding="utf-8", newline="\n") as out:
                    out.write(text)


if __name__ == "__main__":
    main()
