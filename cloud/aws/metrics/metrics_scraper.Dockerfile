FROM public.ecr.aws/aws-observability/aws-otel-collector:latest
COPY otel-config.yaml /etc/ecs/otel-config.yaml
CMD ["--config=/etc/ecs/otel-config.yaml"]
