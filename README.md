# Sistema de Gestión de Guías de Despacho
### CDY2204 – Desarrollo Cloud Native | Semana 3 | Exp. 1

API REST con Spring Boot 3.2 que gestiona guías de despacho, integrando **AWS EFS** para almacenamiento temporal y **AWS S3** para almacenamiento persistente, desplegada automáticamente en **EC2** mediante un pipeline CI/CD con **GitHub Actions**.

---

## Tecnologías

| Tecnología | Versión | Rol |
|---|---|---|
| Java | 17 | Lenguaje |
| Spring Boot | 3.2.5 | Framework |
| iText 7 | 7.2.5 | Generación de PDF |
| AWS SDK v2 | 2.25.40 | Integración S3 |
| H2 Database | Runtime | Base de datos embebida |
| Docker | Latest | Contenedorización |
| GitHub Actions | - | CI/CD |

---

## Arquitectura

```
[Cliente] → [REST API :8080]
                ↓
         [GuiaDespachoService]
          /         |        \
    [PdfService] [EfsService] [S3Service]
                   ↓              ↓
              /app/efs      s3://bucket/
           (temporal)    yyyyMMdd/transportista/guia.pdf
```

---

## Endpoints REST

### Crear guía (genera PDF, guarda en EFS y sube a S3)
```
POST /api/guias
Content-Type: application/json

{
  "transportista": "Transportes XYZ",
  "destinatario": "Juan Pérez",
  "direccionDestino": "Av. Providencia 1234, Santiago",
  "descripcionCarga": "Electrónica - 10 cajas",
  "pesoKg": 45.5,
  "fechaDespacho": "2024-01-15"
}
```

### Obtener guía por número
```
GET /api/guias/{numeroGuia}
```

### Descargar PDF desde S3
```
GET /api/guias/{numeroGuia}/descargar
```

### Actualizar guía (regenera PDF y actualiza en S3)
```
PUT /api/guias/{numeroGuia}
Content-Type: application/json

{
  "destinatario": "María González",
  "pesoKg": 50.0
}
```

### Eliminar guía (elimina de S3, EFS y BD)
```
DELETE /api/guias/{numeroGuia}
```

### Consultar historial (con filtros)
```
GET /api/guias/historial
GET /api/guias/historial?transportista=Transportes XYZ
GET /api/guias/historial?fecha=2024-01-15
GET /api/guias/historial?transportista=Transportes XYZ&fecha=2024-01-15
```

### Health check
```
GET /api/guias/health
```

---

## Organización en S3

Los archivos se organizan automáticamente:
```
s3://guias-despacho-bucket/
└── 20240115/
    └── transportes_xyz/
        ├── GUIA-20240115-A1B2C3.pdf
        └── GUIA-20240115-D4E5F6.pdf
```

---

## Configuración de Secrets en GitHub

| Secret | Descripción |
|---|---|
| `DOCKERHUB_USERNAME` | Usuario de Docker Hub |
| `DOCKERHUB_TOKEN` | Token de acceso Docker Hub |
| `EC2_HOST` | IP pública de la instancia EC2 |
| `EC2_USER` | Usuario SSH (ej: `ec2-user`) |
| `EC2_SSH_KEY` | Clave privada SSH (PEM completo) |
| `AWS_S3_BUCKET` | Nombre del bucket S3 |
| `AWS_REGION` | Región AWS (ej: `us-east-1`) |
| `AWS_ACCESS_KEY_ID` | Credencial AWS |
| `AWS_SECRET_ACCESS_KEY` | Credencial AWS |
| `AWS_SESSION_TOKEN` | Token de sesión AWS Academy |

---

## Configuración de EFS en EC2

Ejecutar en la instancia EC2 **una sola vez**:

```bash
# Instalar cliente NFS
sudo yum install -y nfs-utils   # Amazon Linux
# o
sudo apt-get install -y nfs-common  # Ubuntu

# Crear directorio de montaje
sudo mkdir -p /mnt/efs

# Montar el EFS (reemplazar con tu DNS de EFS)
sudo mount -t nfs4 \
  -o nfsvers=4.1,rsize=1048576,wsize=1048576,hard,timeo=600,retrans=2,noresvport \
  fs-XXXXXXXXXXXXXXXXX.efs.us-east-1.amazonaws.com:/ /mnt/efs

# Verificar montaje
df -h | grep efs
```

Para que el montaje sea persistente (tras reinicios), agregar a `/etc/fstab`:
```
fs-XXXXXXXXXXXXXXXXX.efs.us-east-1.amazonaws.com:/ /mnt/efs nfs4 nfsvers=4.1,rsize=1048576,wsize=1048576,hard,timeo=600,retrans=2,noresvport,_netdev 0 0
```

---

## Ejecución local

```bash
# Sin AWS (modo local con EFS simulado en carpeta local)
EFS_MOUNT_PATH=./local-efs \
AWS_S3_BUCKET=mi-bucket \
AWS_REGION=us-east-1 \
mvn spring-boot:run
```

---

## Variables de Entorno

| Variable | Valor por defecto | Descripción |
|---|---|---|
| `AWS_S3_BUCKET` | `guias-despacho-bucket` | Bucket S3 |
| `AWS_REGION` | `us-east-1` | Región AWS |
| `EFS_MOUNT_PATH` | `/app/efs` | Ruta EFS en el contenedor |
