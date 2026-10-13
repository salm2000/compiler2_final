#!/usr/bin/env python3
"""Interactive and scriptable project launcher for the restricted web compiler."""

from __future__ import annotations

import argparse
import os
import re
import shutil
import subprocess
import sys
import webbrowser
from pathlib import Path


ROOT = Path(__file__).resolve().parent
EXAMPLES = ROOT / "examples"
TARGET = ROOT / "target"
COMPILER_JAR = TARGET / "compiler.jar"
OUTPUT = ROOT / "output"
REPORTS = ROOT / "reports"


def examples() -> list[str]:
    if not EXAMPLES.is_dir():
        return []
    return sorted(path.name for path in EXAMPLES.iterdir() if path.is_dir() and (path / "app.py").is_file())


def example_path(name: str) -> Path:
    path = EXAMPLES / name
    if name not in examples():
        available = ", ".join(examples()) or "none"
        raise SystemExit(f"Unknown example '{name}'. Available examples: {available}")
    return path


def find_maven() -> str:
    installed = shutil.which("mvn") or shutil.which("mvn.cmd")
    if installed:
        return installed
    executable = "mvn.cmd" if os.name == "nt" else "mvn"
    local = sorted((ROOT / ".tools").glob(f"apache-maven-*/bin/{executable}"), reverse=True)
    if local:
        return str(local[0])
    raise SystemExit("Maven 3.9+ was not found. Install Maven or place it below .tools/apache-maven-*/.")


def run(command: list[str], *, cwd: Path = ROOT, check: bool = True) -> subprocess.CompletedProcess[str]:
    print("\n$ " + " ".join(command), flush=True)
    return subprocess.run(command, cwd=cwd, check=check, text=True)


def jar_is_current() -> bool:
    if not COMPILER_JAR.is_file():
        return False
    inputs = [ROOT / "pom.xml"]
    inputs.extend((ROOT / "src" / "main").rglob("*"))
    newest = max(path.stat().st_mtime for path in inputs if path.is_file())
    return COMPILER_JAR.stat().st_mtime >= newest


def build(*, clean: bool = False, skip_tests: bool = False) -> None:
    goals = ["--no-transfer-progress"]
    if clean:
        goals.append("clean")
    goals.append("package")
    if skip_tests:
        goals.append("-DskipTests")
    run([find_maven(), *goals])
    if not COMPILER_JAR.is_file():
        raise SystemExit(f"Build completed without producing {COMPILER_JAR}")


def ensure_compiler() -> None:
    if not jar_is_current():
        print("Compiler JAR is missing or stale; rebuilding it first.")
        build(skip_tests=True)


def compiler_command(action: str, name: str, *extra: str) -> list[str]:
    project = example_path(name)
    return [
        "java", "-jar", str(COMPILER_JAR), action, str(project / "app.py"),
        "--templates", str(project / "templates"),
        "--css", str(project / "static"),
        *extra,
    ]


def compile_example(name: str) -> Path:
    ensure_compiler()
    destination = OUTPUT / name
    run(compiler_command("compile", name, "--out", str(destination)))
    return destination


def run_example(name: str, port: int) -> None:
    destination = compile_example(name)
    print(f"\nStarting '{name}' at http://127.0.0.1:{port}")
    print("Press Ctrl+C to stop the generated server.\n")
    try:
        run([sys.executable, str(destination / "app.py"), str(port)])
    except KeyboardInterrupt:
        print("\nServer stopped.")


def write_report(name: str, report: str, *, open_browser: bool = False) -> Path:
    ensure_compiler()
    destination = REPORTS / name / f"{report}.html"
    run(compiler_command("symbols" if report == "symbols" else "ast", name, "--html", str(destination)))
    print(f"Report: {destination}")
    if open_browser:
        webbrowser.open(destination.resolve().as_uri())
    return destination


def write_all_reports(name: str, *, open_browser: bool = False) -> tuple[Path, Path]:
    ast = write_report(name, "ast")
    symbols = write_report(name, "symbols")
    if open_browser:
        webbrowser.open(ast.resolve().as_uri())
        webbrowser.open(symbols.resolve().as_uri())
    return ast, symbols


def create_example(name: str) -> Path:
    if not re.fullmatch(r"[A-Za-z][A-Za-z0-9_-]*", name):
        raise SystemExit("Example names must start with a letter and contain only letters, digits, '-' or '_'.")
    destination = EXAMPLES / name
    if destination.exists():
        raise SystemExit(f"Example already exists: {destination}")
    (destination / "templates").mkdir(parents=True)
    (destination / "static").mkdir()
    (destination / "app.py").write_text(
        'app = Application()\n\n@app.route("/", methods=["GET"])\ndef home():\n    return render_template("index.html", title="New example")\n',
        encoding="utf-8",
    )
    (destination / "templates" / "index.html").write_text(
        '<html><head><title>{{ title }}</title><link rel="stylesheet" href="/static/style.css"/></head><body><div class="page"><h1>{{ title }}</h1><p>Edit this example to get started.</p></div></body></html>\n',
        encoding="utf-8",
    )
    (destination / "static" / "style.css").write_text(
        'body { background-color: #eef3f8; color: #334155; margin: 0; padding: 30px; }\n.page { background-color: white; width: 80%; margin: 10px; padding: 24px; border: 1px solid #cbd5e1; }\nh1 { color: #174a7e; }\n',
        encoding="utf-8",
    )
    print(f"Created example scaffold at {destination}")
    return destination


def run_tests() -> None:
    run([find_maven(), "--no-transfer-progress", "test"])


def print_examples() -> None:
    names = examples()
    print("\nAvailable examples:")
    if not names:
        print("  (none)")
    for index, name in enumerate(names, 1):
        print(f"  {index}. {name}")


def choose_example() -> str | None:
    names = examples()
    print_examples()
    if not names:
        return None
    raw = input("Choose an example number (blank to cancel): ").strip()
    if not raw:
        return None
    try:
        return names[int(raw) - 1]
    except (ValueError, IndexError):
        print("Invalid selection.")
        return None


def interactive() -> None:
    actions = {
        "1": "List examples", "2": "Create a new example", "3": "Compile an example",
        "4": "Compile and run an example", "5": "Generate AST HTML", "6": "Generate symbol-table HTML",
        "7": "Generate both HTML reports", "8": "Build compiler", "9": "Run all tests", "0": "Exit",
    }
    while True:
        print("\nRestricted Web Compiler")
        for key, label in actions.items():
            print(f"  {key}. {label}")
        choice = input("Select an action: ").strip()
        try:
            if choice == "0":
                return
            if choice == "1":
                print_examples()
            elif choice == "2":
                name = input("New example name: ").strip()
                if name:
                    create_example(name)
            elif choice in {"3", "4", "5", "6", "7"}:
                name = choose_example()
                if not name:
                    continue
                if choice == "3":
                    compile_example(name)
                elif choice == "4":
                    raw_port = input("Port [8000]: ").strip()
                    run_example(name, int(raw_port or "8000"))
                elif choice == "5":
                    write_report(name, "ast", open_browser=True)
                elif choice == "6":
                    write_report(name, "symbols", open_browser=True)
                else:
                    write_all_reports(name, open_browser=True)
            elif choice == "8":
                build(clean=True)
            elif choice == "9":
                run_tests()
            else:
                print("Unknown action.")
        except (subprocess.CalledProcessError, ValueError) as error:
            print(f"Action failed: {error}", file=sys.stderr)


def parser() -> argparse.ArgumentParser:
    result = argparse.ArgumentParser(description=__doc__)
    sub = result.add_subparsers(dest="command")
    sub.add_parser("list", help="list runnable examples")
    new = sub.add_parser("new", help="create a starter example"); new.add_argument("name")
    compile_parser = sub.add_parser("compile", help="compile an example"); compile_parser.add_argument("example")
    launch = sub.add_parser("run", help="compile and run an example"); launch.add_argument("example"); launch.add_argument("--port", type=int, default=8000)
    for command in ("ast", "symbols", "reports"):
        report = sub.add_parser(command, help=f"generate {command} HTML")
        report.add_argument("example"); report.add_argument("--open", action="store_true", dest="open_browser")
    build_parser = sub.add_parser("build", help="build the compiler JAR"); build_parser.add_argument("--clean", action="store_true")
    sub.add_parser("test", help="run the complete Maven test suite")
    return result


def main() -> None:
    args = parser().parse_args()
    if args.command is None:
        interactive()
    elif args.command == "list":
        print_examples()
    elif args.command == "new":
        create_example(args.name)
    elif args.command == "compile":
        compile_example(args.example)
    elif args.command == "run":
        run_example(args.example, args.port)
    elif args.command in {"ast", "symbols"}:
        write_report(args.example, args.command, open_browser=args.open_browser)
    elif args.command == "reports":
        write_all_reports(args.example, open_browser=args.open_browser)
    elif args.command == "build":
        build(clean=args.clean)
    elif args.command == "test":
        run_tests()


if __name__ == "__main__":
    main()

