#! /usr/bin/env make -f
SUBDIRS = frontend backend backend-grpc backend-rest backend-rest-externo

all: $(SUBDIRS)

run-devel: $(SUBDIRS)
	docker compose -f docker-compose-devel.yml up --build

run: $(SUBDIRS)
	docker compose -f docker-compose.yml up --build

containers: $(SUBDIRS)
	for i in $(SUBDIRS); do \
		$(MAKE) -C $$i $@; \
	done

frontend:
	$(MAKE) -C $@

backend:
	$(MAKE) -C $@

backend-grpc:
	$(MAKE) -C $@

backend-rest:
	$(MAKE) -C $@

backend-rest-externo:
	$(MAKE) -C $@

clean:
	for i in $(SUBDIRS); do \
		$(MAKE) -C $$i $@; \
	done

.PHONY: all run-devel run containers frontend backend backend-grpc backend-rest backend-rest-externo clean
