import firebase_admin
from firebase_admin import credentials, storage

from PIL import Image
import os

cred = credentials.Certificate("sa-g4-a91ed-ecd15449f098.json")
firebase_admin.initialize_app(cred, {'storageBucket': 'sa-g4-a91ed.appspot.com'})

folder_path = 'data'
final_size = (128, 256)

bucket = storage.bucket()


def download_images():
    blobs = bucket.list_blobs(prefix=folder_path)
    i = 0
    for blob in blobs:
        if not blob.name.endswith('/'):
            print("Downloading image {}...".format(i))
            i += 1

            file_name = os.path.basename(blob.name)
            blob.download_to_filename(os.path.join(folder_path, file_name))

# Rotate images 90 degrees clockwise
def rotate_images():
    for filename in os.listdir(folder_path):
        if filename.endswith(".jpg"):
            print("Rotating image {}...".format(filename))
            im = Image.open(os.path.join(folder_path, filename))
            im.rotate(90, expand=True).save(os.path.join(folder_path, filename))


# Upload images to Firebase Storage
def upload_images():
    for filename in os.listdir(folder_path):
        if filename.endswith(".jpg"):
            print("Uploading image {}...".format(filename))
            blob = bucket.blob(folder_path + '/' + filename)
            blob.upload_from_filename(os.path.join(folder_path, filename))

def check_size(folder_name, width=128, height=256):
    all_same_size = True
    path = os.path.join(folder_path, folder_name)
    for filename in os.listdir(path):
        if filename.endswith(".jpg"):
            im = Image.open(os.path.join(path, filename))
            img_width, img_height = im.size
            if img_width != width or img_height != height:
                print("Image {} is not {}x{} pixels.".format(filename, width, height))
                all_same_size = False
    
    if all_same_size:
        print("All images are {}x{} pixels.".format(width, height))

# Name of the image is "1234567.jpg" (numbers followed by .jpg)
# Change the name to "1234567_jumping.jpg" (numbers followed by _jumping.jpg) or "1234567_not_jumping.jpg"
# depending on the folder the image is in (jumping or not_jumping)
def rename_images(folder_name):
    path = os.path.join(folder_path, folder_name)
    for filename in os.listdir(path):
        if filename.endswith(".jpg"):
            print("Renaming image {}...".format(filename))
            os.rename(os.path.join(path, filename), os.path.join(path, filename[:-4] + '_' + folder_name + '.jpg'))


# Crop borders of images
def crop_images(folder_name):
    path = os.path.join(folder_path, folder_name)
    for filename in os.listdir(path):
        if filename.endswith(".jpg"):
            print("Cropping image {}...".format(filename))
            im = Image.open(os.path.join(path, filename))

            # Get dimensions
            width, height = im.size

            # Crop 80 pixels from left, 80 pixels from right, 0 pixels from top and 100 pixels from bottom
            crop_left, crop_right, crop_top, crop_bottom = 80, width - 80, 0, height - 100
            im = im.crop((crop_left, crop_top, crop_right, crop_bottom))

            im.save(os.path.join(path, filename))

# Resize images to 128x256 pixels
def resize_images(folder_name):
    path = os.path.join(folder_path, folder_name)
    for filename in os.listdir(path):
        if filename.endswith(".jpg"):
            print("Resizing image {}...".format(filename))
            im = Image.open(os.path.join(path, filename))
            im = im.resize(final_size)
            im.save(os.path.join(path, filename))

