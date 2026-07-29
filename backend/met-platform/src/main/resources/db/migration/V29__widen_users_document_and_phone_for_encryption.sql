-- document_number y phone se cifran de forma determinista (AES/CBC, IV 16 bytes + bloques)
-- antes de guardarse (ver SearchableCryptoConverter). El texto cifrado en base64 excede
-- ampliamente los VARCHAR(20) originales (pensados para el valor en texto plano), por lo que
-- cualquier INSERT/UPDATE con cifrado real fallaba con "value too long for type character
-- varying(20)". Se amplía a 255 para que coincida con la anotación @Column de UserJpaEntity.
ALTER TABLE users ALTER COLUMN document_number TYPE VARCHAR(255);
ALTER TABLE users ALTER COLUMN phone TYPE VARCHAR(255);
