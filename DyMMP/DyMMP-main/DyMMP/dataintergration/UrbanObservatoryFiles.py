import os.path
import shutil


def dowload_and_extract(http_path, experiment_name, filename_dst):
    from DyMMP.dataintergration.ReadFileConverter import url2filename
    from DyMMP.dataintergration.ReadFileConverter import download
    from pathlib import Path
    if not os.path.isfile(os.path.join("data", experiment_name, filename_dst)):
        extract_path = Path(os.path.join("data", experiment_name, filename_dst))
        folder = os.path.join("data", experiment_name, extract_path.stem)
        folder_path = Path(folder)
        dst = os.path.join("data", experiment_name, url2filename(http_path))
        if not os.path.isdir(folder):
            if not os.path.exists(dst):
                download(http_path, dst)
            shutil.unpack_archive(dst, folder)
            os.unlink(dst)
        files = [f for f in folder_path.iterdir() if f.is_file()]
        assert len(files) == 1
        file = files[0]
        shutil.move(str(files[0]), extract_path)
        folder_path.rmdir()

