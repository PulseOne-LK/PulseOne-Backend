# Build Docker images for local services
docker_build('auth-service', './auth-service')
docker_build('api-gateway', './api-gateway')

# Load Kubernetes YAML files
k8s_yaml([
    'auth-postgres-db-deployment.yaml',
    'auth-postgres-db-service.yaml',
    'auth-postgres-data-persistentvolumeclaim.yaml',
    'auth-service-pod.yaml',
    'api-gateway-pod.yaml',
    'api-gateway-service.yaml',
])

# Optionally, you can enable live updates for faster dev cycles
# For example:
# live_update('auth-service', [
#     sync('./auth-service', '/app'),
#     restart_container(),
# ])