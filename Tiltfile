trigger_mode(TRIGGER_MODE_MANUAL)

# Auth Service
docker_build('auth-service', './auth-service')

k8s_yaml([
    'auth-postgres-data-persistentvolumeclaim.yaml',
    'auth-postgres-db-deployment.yaml',
    'auth-postgres-db-service.yaml',
    'auth-service-pod.yaml',
    'auth-service-service.yaml'
])

# Profile Service
docker_build('profile-service', './profile-service')
k8s_yaml([
    'profile-postgres-data-persistentvolumeclaim.yaml',
    'profile-postgres-db-deployment.yaml',
    'profile-postgres-db-service.yaml',
    'profile-service-pod.yaml',
    'profile-service-service.yaml'
])

# API Gateway
docker_build('api-gateway', './api-gateway')
k8s_yaml([
    'api-gateway-pod.yaml',
    'api-gateway-service.yaml'
])

k8s_resource('auth-service', port_forwards='8080:8080')
k8s_resource('profile-service', port_forwards='8082:8080')
k8s_resource('api-gateway', port_forwards='8081:8081')
k8s_resource('auth-postgres-db', port_forwards='5433:5432')
k8s_resource('profile-postgres-db', port_forwards='5434:5432')

# Optionally, you can enable live updates for faster dev cycles
# For example:
# live_update('auth-service', [
#     sync('./auth-service', '/app'),
#     restart_container(),
# ])