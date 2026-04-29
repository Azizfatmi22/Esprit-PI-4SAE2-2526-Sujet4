-- Script SQL pour mettre à jour la colonne method dans la table payments
-- Si la colonne est un ENUM, on la convertit en VARCHAR
-- Si c'est déjà un VARCHAR, on augmente sa taille

ALTER TABLE payments 
MODIFY COLUMN method VARCHAR(50) NOT NULL;
