-- Create the gxa_marker_gene table if it doesn't exist
CREATE TABLE IF NOT EXISTS gxa_marker_gene
(
    experiment_accession VARCHAR(255) NOT NULL CHECK (experiment_accession LIKE 'E-%'),
    assay VARCHAR(255) NOT NULL,
    gene_id VARCHAR(255) NOT NULL,
    specificity_score FLOAT,
    marker_gene_rank INTEGER,
    expression_unit VARCHAR(8) NOT NULL,
    expression_level FLOAT NOT NULL,
    gene_name VARCHAR(255) NOT NULL,
    CONSTRAINT gxa_marker_gene_pkey PRIMARY KEY (experiment_accession, assay, gene_id, expression_unit)
);

-- Insert some test data for E-MTAB-2836
INSERT INTO gxa_marker_gene (experiment_accession, assay, gene_id, specificity_score, marker_gene_rank, expression_unit, expression_level, gene_name)
VALUES ('E-MTAB-2836', 'assay1', 'ENSG00000001', 0.9, 1, 'tpms', 10.5, 'Gene1');
INSERT INTO gxa_marker_gene (experiment_accession, assay, gene_id, specificity_score, marker_gene_rank, expression_unit, expression_level, gene_name)
VALUES ('E-MTAB-2836', 'assay2', 'ENSG00000002', 0.8, 2, 'tpms', 20.3, 'Gene2');
INSERT INTO gxa_marker_gene (experiment_accession, assay, gene_id, specificity_score, marker_gene_rank, expression_unit, expression_level, gene_name)
VALUES ('E-MTAB-2836', 'assay1', 'ENSG00000003', 0.7, 3, 'tpms', 15.2, 'Gene3');
INSERT INTO gxa_marker_gene (experiment_accession, assay, gene_id, specificity_score, marker_gene_rank, expression_unit, expression_level, gene_name)
VALUES ('E-MTAB-2836', 'assay2', 'ENSG00000004', 0.6, 4, 'tpms', 25.1, 'Gene4');
INSERT INTO gxa_marker_gene (experiment_accession, assay, gene_id, specificity_score, marker_gene_rank, expression_unit, expression_level, gene_name)
VALUES ('E-MTAB-2836', 'assay1', 'ENSG00000005', 0.5, 5, 'tpms', 30.7, 'Gene5');
