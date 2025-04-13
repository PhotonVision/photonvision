# gen_api_docs.py

from pathlib import Path
import mkdocs_gen_files

nav = mkdocs_gen_files.Nav()

for path in sorted(Path("photonlibpy").rglob("*.py")):
    if path.name == "py.typed":
        continue

    module_path = path.with_suffix("").as_posix().replace("/", ".")
    parts = tuple(path.relative_to("photonlibpy").with_suffix("").parts)

    if path.name == "__init__.py":
        continue
    else:
        doc_path = Path("reference", *parts).with_suffix(".md")

    nav[parts] = doc_path.as_posix()

    with mkdocs_gen_files.open(doc_path, "w") as f:
        f.write(f"# `{module_path}`\n\n::: {module_path}")

    mkdocs_gen_files.set_edit_path(doc_path, path)

with mkdocs_gen_files.open("reference/SUMMARY.md", "w") as nav_file:
    nav_file.writelines(nav.build_literate_nav())

