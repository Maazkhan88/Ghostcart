import os
from PIL import Image

dir_path = 'mobileui'
try:
    files = sorted([f for f in os.listdir(dir_path) if f.endswith('.png')])
    for file in files:
        file_path = os.path.join(dir_path, file)
        img = Image.open(file_path)
        print(f"=== {file} ===")
        print(f"Format: {img.format}, Size: {img.size}")
        # Print all text metadata keys
        info = img.info
        if info:
            for k, v in info.items():
                # Truncate very long values if needed, but print them if they are prompts
                val_str = str(v)
                if len(val_str) > 1000:
                    val_str = val_str[:1000] + "... (truncated)"
                print(f"  {k}: {val_str}")
        else:
            print("  No metadata info found")
except Exception as e:
    print(f"Error: {e}")
