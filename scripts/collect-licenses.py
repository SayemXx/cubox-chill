"""Bundle the downloaded Rust crates' license notices with the APK."""
import os
import pathlib
import shutil

root = pathlib.Path(__file__).resolve().parent.parent
cargo = pathlib.Path(os.environ.get("CARGO_HOME", pathlib.Path.home() / ".cargo"))
destination = root / "app/src/main/assets/licenses/rust"
count = 0
for registry in (cargo / "registry/src").glob("*"):
    for crate in registry.iterdir():
        if not crate.is_dir():
            continue
        for notice in crate.iterdir():
            if notice.is_file() and notice.name.upper().startswith(("LICENSE", "COPYING", "COPYRIGHT", "NOTICE")):
                target = destination / crate.name / notice.name
                target.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(notice, target)
                count += 1
print(f"Bundled {count} Rust license/notice files")
