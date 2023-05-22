import os
import random
import shutil

def shuffle_and_select_files(path_train, path_test):
    # Get the list of files in the training and testing folders
    filepath_train = os.listdir(path_train)
    filepath_test = os.listdir(path_test)

    # Add the path to the file names
    filenames_train = [os.path.join(path_train, file) for file in filepath_train]
    filenames_test = [os.path.join(path_test, file) for file in filepath_test]
    

    # Merge the file lists
    files = filenames_train + filenames_test


    # Shuffle the file list
    random.shuffle(files)

    # Determine the number of files for training and testing
    num_files_train = 528
    num_files_test = 96

    # Randomly select files for training and testing
    files_train = files[:num_files_train]
    files_test = files[num_files_train:num_files_train+num_files_test]

    os.makedirs('selected_train', exist_ok=True)
    os.makedirs('selected_test', exist_ok=True)

    # Copy the selected files to the selected_train folder
    for file in files_train:
        shutil.copy(file, 'selected_train')

    # Copy the selected files to the selected_test folder
    for file in files_test:
        shutil.copy(file, 'selected_test')
    
# Specify the paths to the training and testing folders
path_train = 'train'
path_test = 'test'


# Shuffle and select files for training and testing
shuffle_and_select_files(path_train, path_test)
