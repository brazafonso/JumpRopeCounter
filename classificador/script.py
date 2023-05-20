import firebase_admin
from firebase_admin import credentials, storage

from PIL import Image
import os

cred = credentials.Certificate("sa-g4-a91ed-ecd15449f098.json")
firebase_admin.initialize_app(cred, {'storageBucket': 'sa-g4-a91ed.appspot.com'})

folder_path = 'data'
final_size = (320, 240)

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

def check_size(folder_name, width=final_size[0], height=final_size[1]):
    all_same_size = True
    path = os.path.join(folder_path, folder_name)
    for filename in os.listdir(path):
        if filename.endswith(".jpg"):
            im = Image.open(os.path.join(path, filename))
            img_width, img_height = im.size
            if img_width != width or img_height != height:
                print("Image {} size is {}x{} pixels instead of {}x{} pixels.".format(filename, img_width, img_height, width, height))
                all_same_size = False
    
    if all_same_size:
        print("All images are {}x{} pixels.".format(width, height))

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


# Rename "frame_XXX.jpg" to "XXX_jumping.jpg" or "XXX_not_jumping.jpg"
#
def rename_images(folder_name, label):
    for filename in os.listdir(os.path.join(folder_path, folder_name)):
        if filename.endswith(".jpg"):
            number_and_extension = filename.replace("frame_", "").split(".")
            new_filename = number_and_extension[0] + "_" + label + "." + number_and_extension[1]
            os.rename(os.path.join(folder_path, folder_name, filename), os.path.join(folder_path, folder_name, new_filename))
