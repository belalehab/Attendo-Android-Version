# PDF Compatibility Prototype & Failure Analysis

## Objective
To generate PDFs containing Arabic (RTL) text using Android's native ndroid.graphics.pdf.PdfDocument without introducing external dependencies like iText.

## Desktop Implementation
The desktop app uses jspdf with a base64-encoded Amiri-Regular.ttf embedded directly into the PDF. This ensures the PDF looks identical on any device, regardless of whether the system has Arabic fonts installed. 

## Native Android Implementation & Exact Failure
Android provides ndroid.graphics.pdf.PdfDocument, which creates a Canvas that you can draw text onto using Canvas.drawText(). 

1. **RTL Shaping**: Android's Canvas (via Minikin/HarfBuzz) handles Arabic RTL shaping automatically.
2. **Font Embedding (The Failure Point)**: ndroid.graphics.pdf.PdfDocument **does not support font embedding**. When a custom Typeface (such as Amiri-Regular.ttf) is used with Canvas.drawText(), the generated PDF file only contains a reference to the font, or it draws the text using standard system fonts if the referenced font cannot be embedded. 
3. **Consequence**: When the exported PDF is shared to a Windows PC or printed, the Arabic text will either use a fallback system font (breaking visual parity with the desktop) or render incorrectly (boxes/missing glyphs) because the PC lacks Amiri-Regular.

## Conclusion
Native Android PDF rendering **cannot** reproduce the required result due to the lack of font embedding support. 
**Status**: NATIVE POC BLOCKED/FAILED. 

## Proposed Solution
We must evaluate **iText 7** or **Apache PDFBox** for Android, as these libraries explicitly support TrueType font embedding (PdfFontFactory.createFont()), which is a hard requirement for visual parity with the desktop exports.
