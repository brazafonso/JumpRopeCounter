import firebase_admin
from firebase_admin import credentials, storage

from PIL import Image
import os

cred = credentials.Certificate("sa-g4-a91ed-ecd15449f098.json")
firebase_admin.initialize_app(cred, {'storageBucket': 'sa-g4-a91ed.appspot.com'})

folder_path = 'data'

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

def check_size():
    all_same_size = True
    for filename in os.listdir(folder_path):
        if filename.endswith(".jpg"):
            im = Image.open(os.path.join(folder_path, filename))
            if im.size[0] != 480 or im.size[1] != 640:
                print("Check size of image {}!".format(filename))
                all_same_size = False
    
    if all_same_size:
        print("All images are 480x640 pixels.")

