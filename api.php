<?php
// ===== CONFIGURACIÓN DE ENCABEZADOS Y CORS =====
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With");
header("Content-Type: application/json; charset=UTF-8");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

// ===== CONEXIÓN A BASE DE DATOS (PDO) =====

$host    = getenv('DB_HOST') ?: '';
$port    = getenv('DB_PORT') ?: '';
$db      = getenv('DB_NAME') ?: '';
$user    = getenv('DB_USER') ?: '';
$pass    = getenv('DB_PASS') ?: '';
$charset = 'utf8mb4';

$dsn = "mysql:host=$host;port=$port;dbname=$db;charset=$charset";
$options = [
    PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
    PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
    PDO::ATTR_EMULATE_PREPARES   => false,
];

try {
    $pdo = new PDO($dsn, $user, $pass, $options);
} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(["ok" => false, "error" => "Error de conexión a la BD: " . $e->getMessage()]);
    exit();
}

$method = $_SERVER['REQUEST_METHOD'];
$uri    = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);
$body   = json_decode(file_get_contents("php://input"), true) ?? [];

function formatearProducto($p) {
    return [
        "ID"              => (int)($p['ID'] ?? $p['ID_Producto']),
        "ID_Producto"     => (int)($p['ID_Producto'] ?? $p['ID']),
        "Producto"        => $p['Producto'],
        "ID_Categoria"    => (int)($p['ID_Categoria'] ?? 1),
        "Categoria"       => $p['Categoria'] ?? "General",
        "PCompra"         => (float)($p['PCompra'] ?? 0.0),
        "PVenta"          => (float)($p['PVenta'] ?? 0.0),
        "StockActual"     => (int)($p['StockActual'] ?? 0),
        "StockMinimo"     => (int)($p['StockMinimo'] ?? 0),
        "Descripcion"     => $p['Descripcion'] ?? "",
        "NombreImagen"    => $p['NombreImagen'] ?? "default.png",
        "FeUltim"         => $p['FeUltim'] ?? null,
        "Activo"          => (int)($p['Activo'] ?? 1),
        "MostarEmpleado"  => (int)($p['MostarEmpleado'] ?? 1)
    ];
}

// ===================================================================
// 1. ENDPOINT PARA RETIRAR STOCK Y REGISTRAR EN REGISTRORETIRO (/api/retirar)
// ===================================================================
if ((strpos($uri, '/api/retirar') !== false || strpos($uri, '/api/retiros') !== false) && $method === 'POST') {
    try {
        $ID_Producto      = $body['ID_Producto'] ?? $body['id'] ?? null;
        $CantidadARetirar = (int)($body['cantidad'] ?? $body['StockRetirado'] ?? 0);
        $Motivo           = $body['motivo'] ?? $body['Motivo'] ?? 'Salida de Inventario';
        $ID_Usuario       = $body['ID_Usuario'] ?? 1;

        if (!$ID_Producto || $CantidadARetirar <= 0) {
            http_response_code(400);
            echo json_encode(["ok" => false, "error" => "Se requiere ID_Producto y una cantidad válida a retirar"]);
            exit();
        }

        $pdo->beginTransaction();

        // Consultar stock actual (bloquea la fila hasta terminar)
        $stmt = $pdo->prepare("SELECT StockActual, Producto FROM productos WHERE ID_Producto = ? FOR UPDATE");
        $stmt->execute([$ID_Producto]);
        $prod = $stmt->fetch();

        if (!$prod) {
            $pdo->rollBack();
            http_response_code(404);
            echo json_encode(["ok" => false, "error" => "Producto no encontrado"]);
            exit();
        }

        if ($prod['StockActual'] < $CantidadARetirar) {
            $pdo->rollBack();
            http_response_code(400);
            echo json_encode(["ok" => false, "error" => "Stock insuficiente. Disponible: " . $prod['StockActual']]);
            exit();
        }

        // Actualizar Stock en Productos
        $nuevoStock = $prod['StockActual'] - $CantidadARetirar;
        $updateStmt = $pdo->prepare("UPDATE productos SET StockActual = ?, FeUltim = NOW() WHERE ID_Producto = ?");
        $updateStmt->execute([$nuevoStock, $ID_Producto]);

        // Registrar en la tabla registroretiro
        $insertLog = $pdo->prepare("INSERT INTO registroretiro (ID_Producto, StockRetirado, Fecha, Motivo, ID_Usuario) VALUES (?, ?, NOW(), ?, ?)");
        $insertLog->execute([$ID_Producto, $CantidadARetirar, $Motivo, $ID_Usuario]);

        $pdo->commit();

        echo json_encode([
            "ok" => true,
            "mensaje" => "Se retiraron $CantidadARetirar unidades de " . $prod['Producto'],
            "nuevoStock" => $nuevoStock
        ]);
    } catch (PDOException $e) {
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        http_response_code(500);
        echo json_encode(["ok" => false, "error" => $e->getMessage()]);
    }
    exit();
}

// ===================================================================
// 2. ENDPOINTS DE PRODUCTOS (/api/productos)
// ===================================================================
if (strpos($uri, '/api/productos') !== false) {

    switch ($method) {

        case 'GET':
            try {
                $id = $_GET['id'] ?? null;

                if ($id) {
                    $sql = "SELECT
                                p.ID_Producto AS ID,
                                p.ID_Producto,
                                p.Producto,
                                p.ID_Categoria,
                                COALESCE(c.Categoria, 'General') AS Categoria,
                                p.PCompra,
                                p.PVenta,
                                p.StockActual,
                                p.StockMinimo,
                                p.Descripcion,
                                COALESCE(i.NombreImagen, 'default.png') AS NombreImagen,
                                p.FeUltim,
                                p.Activo,
                                p.MostarEmpleado
                            FROM productos p
                            LEFT JOIN categoria c ON p.ID_Categoria = c.ID_categoria
                            LEFT JOIN imagenes i ON p.ID_Producto = i.ID_Producto
                            WHERE p.ID_Producto = ?";

                    $stmt = $pdo->prepare($sql);
                    $stmt->execute([$id]);
                    $row = $stmt->fetch();

                    if ($row) {
                        echo json_encode(["ok" => true, "data" => [formatearProducto($row)]]);
                    } else {
                        http_response_code(404);
                        echo json_encode(["ok" => false, "error" => "Producto no encontrado"]);
                    }
                } else {
                    $sql = "SELECT
                                p.ID_Producto AS ID,
                                p.ID_Producto,
                                p.Producto,
                                p.ID_Categoria,
                                COALESCE(c.Categoria, 'General') AS Categoria,
                                p.PCompra,
                                p.PVenta,
                                p.StockActual,
                                p.StockMinimo,
                                p.Descripcion,
                                COALESCE(i.NombreImagen, 'default.png') AS NombreImagen,
                                p.FeUltim,
                                p.Activo,
                                p.MostarEmpleado
                            FROM productos p
                            LEFT JOIN categoria c ON p.ID_Categoria = c.ID_categoria
                            LEFT JOIN imagenes i ON p.ID_Producto = i.ID_Producto
                            ORDER BY p.ID_Producto DESC";

                    $stmt = $pdo->query($sql);
                    $rows = $stmt->fetchAll();

                    $formattedRows = array_map('formatearProducto', $rows);

                    echo json_encode(["ok" => true, "total" => count($formattedRows), "data" => $formattedRows]);
                }
            } catch (PDOException $e) {
                http_response_code(500);
                echo json_encode(["ok" => false, "error" => $e->getMessage()]);
            }
            break;

        case 'POST':
            try {
                $Producto     = trim($body['Producto'] ?? $body['nombre'] ?? '');
                $ID_Categoria = (int)($body['ID_Categoria'] ?? $body['idCategoria'] ?? 1);
                $PCompra      = (float)($body['PCompra'] ?? $body['precioCompra'] ?? 0.0);
                $PVenta       = (float)($body['PVenta'] ?? $body['precioVenta'] ?? 0.0);
                $StockActual  = (int)($body['StockActual'] ?? $body['stockActual'] ?? 0);
                $StockMinimo  = (int)($body['StockMinimo'] ?? $body['stockMinimo'] ?? 0);
                $Descripcion  = trim($body['Descripcion'] ?? $body['descripcion'] ?? '');
                $ID_Usuario   = (int)($body['ID_Usuario'] ?? 1);

                if (empty($Producto)) {
                    http_response_code(400);
                    echo json_encode(["ok" => false, "error" => "El campo 'Producto' es obligatorio"]);
                    exit();
                }

                $pdo->beginTransaction();

                $sql = "INSERT INTO productos
                        (Producto, ID_Categoria, PCompra, PVenta, StockActual, StockMinimo, Descripcion, FeUltim, Activo, MostarEmpleado)
                        VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), 1, 1)";

                $stmt = $pdo->prepare($sql);
                $stmt->execute([$Producto, $ID_Categoria, $PCompra, $PVenta, $StockActual, $StockMinimo, $Descripcion]);

                $idProducto = (int)$pdo->lastInsertId();

                // Registrar el ingreso inicial en el historial
                $insertIngreso = $pdo->prepare(
                    "INSERT INTO registroingreso (ID_Producto, E_N, StockIngresado, Fecha, Motivo, ID_Usuario)
                     VALUES (?, 'E', ?, NOW(), 'Ingreso inicial', ?)"
                );
                $insertIngreso->execute([$idProducto, $StockActual, $ID_Usuario]);

                $pdo->commit();

                http_response_code(201);
                echo json_encode([
                    "ok" => true,
                    "id" => $idProducto,
                    "mensaje" => "Producto creado exitosamente"
                ]);
            } catch (PDOException $e) {
                if ($pdo->inTransaction()) {
                    $pdo->rollBack();
                }
                http_response_code(500);
                echo json_encode(["ok" => false, "error" => $e->getMessage()]);
            }
            break;

        case 'PUT':
            try {
                $ID_Producto  = (int)($body['ID_Producto'] ?? $body['ID'] ?? $body['id'] ?? $_GET['id'] ?? 0);
                $Producto     = trim($body['Producto'] ?? $body['nombre'] ?? '');
                $ID_Categoria = (int)($body['ID_Categoria'] ?? $body['idCategoria'] ?? 1);
                $PCompra      = (float)($body['PCompra'] ?? $body['precioCompra'] ?? 0.0);
                $PVenta       = (float)($body['PVenta'] ?? $body['precioVenta'] ?? 0.0);
                $StockActual  = (int)($body['StockActual'] ?? $body['stockActual'] ?? 0);
                $StockMinimo  = (int)($body['StockMinimo'] ?? $body['stockMinimo'] ?? 0);
                $Descripcion  = trim($body['Descripcion'] ?? $body['descripcion'] ?? '');

                if ($ID_Producto <= 0 || empty($Producto)) {
                    http_response_code(400);
                    echo json_encode(["ok" => false, "error" => "ID_Producto y Producto son campos requeridos"]);
                    exit();
                }

                $sql = "UPDATE productos
                        SET Producto = ?, ID_Categoria = ?, PCompra = ?, PVenta = ?, StockActual = ?, StockMinimo = ?, Descripcion = ?, FeUltim = NOW()
                        WHERE ID_Producto = ?";

                $stmt = $pdo->prepare($sql);
                $stmt->execute([$Producto, $ID_Categoria, $PCompra, $PVenta, $StockActual, $StockMinimo, $Descripcion, $ID_Producto]);

                echo json_encode(["ok" => true, "mensaje" => "Producto actualizado exitosamente"]);
            } catch (PDOException $e) {
                http_response_code(500);
                echo json_encode(["ok" => false, "error" => $e->getMessage()]);
            }
            break;

        case 'DELETE':
            try {
                $ID_Producto = (int)($_GET['id'] ?? $body['ID_Producto'] ?? $body['id'] ?? 0);

                if ($ID_Producto <= 0) {
                    http_response_code(400);
                    echo json_encode(["ok" => false, "error" => "Se requiere un ID_Producto válido para eliminar"]);
                    exit();
                }

                $stmt = $pdo->prepare("DELETE FROM productos WHERE ID_Producto = ?");
                $stmt->execute([$ID_Producto]);

                if ($stmt->rowCount() > 0) {
                    echo json_encode(["ok" => true, "mensaje" => "Producto eliminado exitosamente"]);
                } else {
                    http_response_code(404);
                    echo json_encode(["ok" => false, "error" => "No se encontró el producto a eliminar"]);
                }
            } catch (PDOException $e) {
                http_response_code(500);
                echo json_encode(["ok" => false, "error" => $e->getMessage()]);
            }
            break;
    }
    exit();
}

// ===================================================================
// 3. ENDPOINT DE CATEGORÍAS (/api/categorias)
// ===================================================================
if (strpos($uri, '/api/categorias') !== false) {

    if ($method === 'GET') {
        try {
            $sql = "SELECT ID_categoria, Categoria FROM categoria ORDER BY Categoria ASC";
            $stmt = $pdo->query($sql);
            $rows = $stmt->fetchAll();

            echo json_encode(["ok" => true, "data" => $rows]);
        } catch (PDOException $e) {
            http_response_code(500);
            echo json_encode(["ok" => false, "error" => $e->getMessage()]);
        }
        exit();
    }

    if ($method === 'POST') {
        try {
            $CategoriaNombre = trim($body['Categoria'] ?? $body['nombre'] ?? '');

            if (empty($CategoriaNombre)) {
                http_response_code(400);
                echo json_encode(["ok" => false, "error" => "El nombre de la Categoría es requerido"]);
                exit();
            }

            $sql = "INSERT INTO categoria (Categoria) VALUES (?)";
            $stmt = $pdo->prepare($sql);
            $stmt->execute([$CategoriaNombre]);

            http_response_code(201);
            echo json_encode(["ok" => true, "id" => (int)$pdo->lastInsertId(), "mensaje" => "Categoría creada exitosamente"]);
        } catch (PDOException $e) {
            http_response_code(500);
            echo json_encode(["ok" => false, "error" => $e->getMessage()]);
        }
        exit();
    }
}

// ===================================================================
// 4. ENDPOINTS DE INGRESOS Y RETIROS (/api/ingresos y /api/retiros)
// ===================================================================
if (strpos($uri, '/api/ingresos') !== false && $method === 'GET') {
    try {
        $sql = "SELECT
                    r.ID_Ringreso AS ID,
                    p.ID_Producto,
                    p.Producto,
                    r.E_N,
                    r.StockIngresado,
                    r.Fecha,
                    r.Motivo,
                    COALESCE(l.usuario, 'Sistema') AS usuario
                FROM registroingreso r
                INNER JOIN productos p ON r.ID_Producto = p.ID_Producto
                LEFT JOIN login l ON r.ID_Usuario = l.ID_Usuario
                ORDER BY r.ID_Ringreso DESC";

        $stmt = $pdo->query($sql);
        $rows = $stmt->fetchAll();

        echo json_encode(["ok" => true, "total" => count($rows), "data" => $rows]);
    } catch (PDOException $e) {
        http_response_code(500);
        echo json_encode(["ok" => false, "error" => $e->getMessage()]);
    }
    exit();
}

// POST /api/ingresos (REGISTRAR INGRESO EN EL HISTORIAL)
if (strpos($uri, '/api/ingresos') !== false && $method === 'POST') {
    try {
        $ID_Producto    = $body['ID_Producto'] ?? null;
        $E_N            = $body['E_N'] ?? null;
        $StockIngresado = $body['StockIngresado'] ?? null;
        $Motivo         = $body['Motivo'] ?? null;
        $ID_Usuario     = $body['ID_Usuario'] ?? null;

        if (!$ID_Producto || !$E_N || !$StockIngresado || !$ID_Usuario) {
            http_response_code(400);
            echo json_encode([
                "ok" => false,
                "error" => "Faltan campos: ID_Producto, E_N, StockIngresado, ID_Usuario"
            ]);
            exit();
        }

        $sql = "INSERT INTO registroingreso (ID_Producto, E_N, StockIngresado, Fecha, Motivo, ID_Usuario)
                VALUES (?, ?, ?, NOW(), ?, ?)";

        $stmt = $pdo->prepare($sql);
        $stmt->execute([$ID_Producto, $E_N, $StockIngresado, $Motivo, $ID_Usuario]);

        http_response_code(201);
        echo json_encode(["ok" => true, "id" => (int)$pdo->lastInsertId()]);
    } catch (PDOException $e) {
        http_response_code(500);
        echo json_encode(["ok" => false, "error" => $e->getMessage()]);
    }
    exit();
}

if (strpos($uri, '/api/retiros') !== false && $method === 'GET') {
    try {
        $sql = "SELECT
                    r.ID_Rretiro AS ID,
                    p.ID_Producto,
                    p.Producto,
                    r.StockRetirado,
                    r.Fecha,
                    r.Motivo
                FROM registroretiro r
                INNER JOIN productos p ON r.ID_Producto = p.ID_Producto
                ORDER BY r.ID_Rretiro DESC";

        $stmt = $pdo->query($sql);
        $rows = $stmt->fetchAll();

        echo json_encode(["ok" => true, "total" => count($rows), "data" => $rows]);
    } catch (PDOException $e) {
        http_response_code(500);
        echo json_encode(["ok" => false, "error" => $e->getMessage()]);
    }
    exit();
}

// ===================================================================
// ENDPOINT DE LOGIN (/api/login)
// ===================================================================
if (strpos($uri, '/api/login') !== false && $method === 'POST') {
    try {
        $usuario = trim($body['usuario'] ?? '');
        $password = trim($body['password'] ?? '');

        if (empty($usuario) || empty($password)) {
            http_response_code(400);
            echo json_encode(["ok" => false, "error" => "Usuario y contraseña son requeridos"]);
            exit();
        }

        $stmt = $pdo->prepare("SELECT ID_Usuario, usuario, password, nombre FROM login WHERE usuario = ? LIMIT 1");
        $stmt->execute([$usuario]);
        $user = $stmt->fetch();

        if (!$user) {
            http_response_code(401);
            echo json_encode(["ok" => false, "error" => "Usuario o contraseña incorrectos"]);
            exit();
        }

        if ($user['password'] !== $password) {
            http_response_code(401);
            echo json_encode(["ok" => false, "error" => "Usuario o contraseña incorrectos"]);
            exit();
        }

        $token = bin2hex(random_bytes(16));

        echo json_encode([
            "ok" => true,
            "data" => [
                "token" => $token,
                "usuario" => [
                    "ID_Usuario" => (int)$user['ID_Usuario'],
                    "usuario" => $user['usuario'],
                    "password" => $user['password'],
                    "nombre" => $user['nombre']
                ],
                "mensaje" => "Inicio de sesión exitoso"
            ]
        ]);
    } catch (PDOException $e) {
        http_response_code(500);
        echo json_encode(["ok" => false, "error" => $e->getMessage()]);
    }
    exit();
}

// Ruta no encontrada
http_response_code(404);
echo json_encode(["ok" => false, "error" => "Ruta no encontrada"]);
