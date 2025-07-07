# SSF-CONLLU_Conversion
## [Note: To run this convertor, make sure the root folder is renamed to "Sanchay"]
This Java project converts linguistically annotated data from the Shakti Standard Format (SSF) to the CoNLL-U format, commonly used in Universal Dependencies (UD) parsing and NLP tasks.

Batch Conversion: Converts all .ssf files in a specified folder to .conllu files with the same base name.

Interactive: Prompts the user for the folder path at runtime.

## Requirements:
Java 8 or higher

Sanchay Library (Download and paste in lib/)

## Follow the Prompt
When the program starts, you’ll see:
  Enter the folder path containing .ssf files:
Enter the path to the folder containing your .ssf files. The program will convert each .ssf file to a .conllu file in the same folder and display a progress bar.

## Output
For each filename.ssf in the folder, a filename.conllu will be created in the same folder.

## Project Structure
Sanchay/
├── lib/                # JAR dependencies
├── props/              # Sanchay property files
├── src/
│   └── SSFToCoNLLUConverter.java
├── ...
