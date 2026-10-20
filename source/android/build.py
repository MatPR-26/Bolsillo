"""Windows build compatibility entry point. See build.ps1 for SDK/JDK parameters."""
import pathlib
import shutil
import subprocess
import sys
script = pathlib.Path(__file__).resolve().with_name("build.ps1")
shell = shutil.which("pwsh") or shutil.which("powershell")
if not shell:
    sys.exit("This build uses PowerShell and the Android SDK on Windows. Run android/build.ps1.")
subprocess.run([shell, "-NoProfile", "-File", str(script), *sys.argv[1:]], check=True)
