LATEST_TAG = $(shell git describe --tags --abbrev=0 2>/dev/null || echo "no-tag-found")

.PHONY: latest
latest:
	@echo $(LATEST_TAG)