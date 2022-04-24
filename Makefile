ZIPNAME=22002643.zip

Report.pdf: Report.tex
	latexmk -pdf

.PHONY: zip clean test

test: Solution.java
	java Solution.java

zip: Report.pdf Solution.java
	7z a -tzip $(ZIPNAME) Report.pdf Solution.java
	7z l $(ZIPNAME)

clean:
	rm -f ./*.synctex.gz
	latexmk -c
