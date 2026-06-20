# TODO - Seguridad Alta (Sprint 1)

- [x] sec_001: Eliminar defaults de JWT_SECRET y AES_KEY en `backend/met-platform/src/main/resources/application.yml`
- [x] sec_001: Agregar validación `@PostConstruct` (lanzar excepción si faltan/son inválidos)
- [x] sec_003: Crear `RefreshTokenRepositoryPort` + adaptador de persistencia
- [x] sec_003: Modificar `JwtTokenAdapter.java` para persistir/consultar revocación de refresh tokens
- [x] sec_003: Ajustar flujo de refresh (usecase/controlador) para rotación y revocación
- [x] sec_004: Modificar `AuthRateLimitFilter.java` a rate limiting por usuario + IP usando Redis con TTL por ventana
- [x] sec_004: Integrar contadores Redis y prefijos por IP/usuario
- [ ] Ejecutar `mvn test` en `backend/met-platform`
