INSERT INTO products (
    id, name, slug, description, brand, collection_name, category,
    base_price, discount_percentage, status, created_at, updated_at
) VALUES
    ('10000000-0000-0000-0000-000000000001', 'Linen Horizon Shirt', 'linen-horizon-shirt', 'Relaxed linen shirt with a clean camp collar.', 'Verse', 'Coastal Lines', 'TOPS', 79.90, 0, 'ACTIVE', '2026-01-15T10:00:00Z', '2026-01-15T10:00:00Z'),
    ('10000000-0000-0000-0000-000000000002', 'Drift Wide Trousers', 'drift-wide-trousers', 'Wide-leg trousers cut for an easy everyday silhouette.', 'Verse', 'Coastal Lines', 'BOTTOMS', 109.90, 10, 'ACTIVE', '2026-01-15T10:05:00Z', '2026-01-15T10:05:00Z'),
    ('10000000-0000-0000-0000-000000000003', 'Dune Wrap Dress', 'dune-wrap-dress', 'Midi wrap dress with adjustable waist ties.', 'Verse', 'Coastal Lines', 'DRESSES', 139.90, 0, 'ACTIVE', '2026-01-15T10:10:00Z', '2026-01-15T10:10:00Z'),
    ('10000000-0000-0000-0000-000000000004', 'Northfield Overshirt', 'northfield-overshirt', 'Structured overshirt designed for transitional layering.', 'Verse', 'Quiet Terrain', 'OUTERWEAR', 159.90, 15, 'ACTIVE', '2026-02-01T09:00:00Z', '2026-02-01T09:00:00Z'),
    ('10000000-0000-0000-0000-000000000005', 'Moss Rib Cardigan', 'moss-rib-cardigan', 'Soft rib-knit cardigan with a relaxed fit.', 'Verse', 'Quiet Terrain', 'KNITWEAR', 119.90, 0, 'DRAFT', '2026-02-01T09:05:00Z', '2026-02-01T09:05:00Z'),
    ('10000000-0000-0000-0000-000000000006', 'Contour Canvas Tote', 'contour-canvas-tote', 'Durable canvas tote with an internal essentials pocket.', 'Verse', 'Quiet Terrain', 'ACCESSORIES', 49.90, 0, 'DRAFT', '2026-02-01T09:10:00Z', '2026-02-01T09:10:00Z');

INSERT INTO product_variants (
    id, product_id, sku, size, color_name, color_hex, stock_quantity, created_at, updated_at
) VALUES
    ('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'VRS-HZN-S-SAND', 'S', 'Sand', '#D8C3A5', 12, '2026-01-15T10:00:00Z', '2026-01-15T10:00:00Z'),
    ('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'VRS-HZN-M-SAND', 'M', 'Sand', '#D8C3A5', 18, '2026-01-15T10:00:00Z', '2026-01-15T10:00:00Z'),
    ('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', 'VRS-HZN-M-SKY', 'M', 'Sky', '#A9C8D8', 9, '2026-01-15T10:00:00Z', '2026-01-15T10:00:00Z'),
    ('20000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000002', 'VRS-DRF-38-INK', '38', 'Ink', '#26313B', 8, '2026-01-15T10:05:00Z', '2026-01-15T10:05:00Z'),
    ('20000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000002', 'VRS-DRF-40-INK', '40', 'Ink', '#26313B', 11, '2026-01-15T10:05:00Z', '2026-01-15T10:05:00Z'),
    ('20000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000003', 'VRS-DUN-S-CLAY', 'S', 'Clay', '#B56F52', 6, '2026-01-15T10:10:00Z', '2026-01-15T10:10:00Z'),
    ('20000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000003', 'VRS-DUN-M-CLAY', 'M', 'Clay', '#B56F52', 10, '2026-01-15T10:10:00Z', '2026-01-15T10:10:00Z'),
    ('20000000-0000-0000-0000-000000000008', '10000000-0000-0000-0000-000000000004', 'VRS-NTH-M-STONE', 'M', 'Stone', '#817C70', 7, '2026-02-01T09:00:00Z', '2026-02-01T09:00:00Z'),
    ('20000000-0000-0000-0000-000000000009', '10000000-0000-0000-0000-000000000004', 'VRS-NTH-L-STONE', 'L', 'Stone', '#817C70', 5, '2026-02-01T09:00:00Z', '2026-02-01T09:00:00Z'),
    ('20000000-0000-0000-0000-000000000010', '10000000-0000-0000-0000-000000000005', 'VRS-MOS-XS-MOSS', 'XS', 'Moss', '#68745A', 0, '2026-02-01T09:05:00Z', '2026-02-01T09:05:00Z'),
    ('20000000-0000-0000-0000-000000000011', '10000000-0000-0000-0000-000000000005', 'VRS-MOS-S-MOSS', 'S', 'Moss', '#68745A', 0, '2026-02-01T09:05:00Z', '2026-02-01T09:05:00Z'),
    ('20000000-0000-0000-0000-000000000012', '10000000-0000-0000-0000-000000000006', 'VRS-CNT-OS-NAT', 'ONE SIZE', 'Natural', '#E4D8C4', 14, '2026-02-01T09:10:00Z', '2026-02-01T09:10:00Z');

INSERT INTO product_images (
    id, product_id, url, alt_text, display_order, primary_image, created_at
) VALUES
    ('30000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'https://images.example.com/verse/linen-horizon-shirt-front.jpg', 'Linen Horizon Shirt in Sand, front view', 0, TRUE, '2026-01-15T10:00:00Z'),
    ('30000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'https://images.example.com/verse/linen-horizon-shirt-detail.jpg', 'Linen Horizon Shirt collar detail', 1, FALSE, '2026-01-15T10:00:00Z'),
    ('30000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000002', 'https://images.example.com/verse/drift-wide-trousers-front.jpg', 'Drift Wide Trousers in Ink, front view', 0, TRUE, '2026-01-15T10:05:00Z'),
    ('30000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000003', 'https://images.example.com/verse/dune-wrap-dress-front.jpg', 'Dune Wrap Dress in Clay, front view', 0, TRUE, '2026-01-15T10:10:00Z'),
    ('30000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000004', 'https://images.example.com/verse/northfield-overshirt-front.jpg', 'Northfield Overshirt in Stone, front view', 0, TRUE, '2026-02-01T09:00:00Z'),
    ('30000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000005', 'https://images.example.com/verse/moss-rib-cardigan-front.jpg', 'Moss Rib Cardigan, front view', 0, TRUE, '2026-02-01T09:05:00Z'),
    ('30000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000006', 'https://images.example.com/verse/contour-canvas-tote-front.jpg', 'Contour Canvas Tote in Natural', 0, TRUE, '2026-02-01T09:10:00Z');
