package com.duocuc.cursos.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para operaciones con AWS S3.
 * Organiza archivos en carpetas: /{fecha}/{instructor}/{codigoCurso}.pdf
 */
@Slf4j
@Service
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * Construye la clave S3 organizada por fecha y instructor.
     * Ejemplo: 20240115/instructor_x/CURSO-001.pdf
     */
    public String construirClaveS3(LocalDate fecha, String instructor, String codigoCurso) {
        String fechaStr = fecha.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String instructorNorm = instructor.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
        return String.format("%s/%s/%s.pdf", fechaStr, instructorNorm, codigoCurso);
    }

    /**
     * Sube un archivo PDF a S3.
     *
     * @param contenido    bytes del PDF
     * @param claveS3      clave (ruta) en S3
     * @return clave S3 del objeto subido
     */
    public String subirArchivo(byte[] contenido, String claveS3) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(claveS3)
                .contentType("application/pdf")
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(contenido));
        log.info("Archivo subido a S3: s3://{}/{}", bucket, claveS3);
        return claveS3;
    }

    /**
     * Descarga un archivo desde S3.
     *
     * @param claveS3 clave del objeto en S3
     * @return bytes del archivo
     */
    public byte[] descargarArchivo(String claveS3) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(claveS3)
                .build();

        ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);
        log.info("Archivo descargado desde S3: s3://{}/{}", bucket, claveS3);
        return response.asByteArray();
    }

    /**
     * Actualiza (sobreescribe) un archivo existente en S3.
     *
     * @param contenido  nuevo contenido
     * @param claveS3    clave del objeto a actualizar
     */
    public void actualizarArchivo(byte[] contenido, String claveS3) {
        // En S3 la actualización es un PUT que sobreescribe el objeto
        subirArchivo(contenido, claveS3);
        log.info("Archivo actualizado en S3: s3://{}/{}", bucket, claveS3);
    }

    /**
     * Elimina un archivo de S3.
     *
     * @param claveS3 clave del objeto a eliminar
     */
    public void eliminarArchivo(String claveS3) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(claveS3)
                .build();

        s3Client.deleteObject(request);
        log.info("Archivo eliminado de S3: s3://{}/{}", bucket, claveS3);
    }

    /**
     * Lista objetos en S3 bajo un prefijo específico (por fecha y/o instructor).
     *
     * @param prefijo prefijo para filtrar (ej: "20240115/instructorX/")
     * @return lista de claves S3
     */
    public List<String> listarArchivos(String prefijo) {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(prefijo)
                .build();

        ListObjectsV2Response response = s3Client.listObjectsV2(request);
        return response.contents().stream()
                .map(S3Object::key)
                .collect(Collectors.toList());
    }

    /**
     * Verifica si un objeto existe en S3.
     */
    public boolean existeArchivo(String claveS3) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(claveS3)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }
}
