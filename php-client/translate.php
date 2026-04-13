<?php
declare(strict_types=1);

/**
 * Minimal PHP client for the Darija Translator REST API (HTTP Basic + JSON).
 */

$text = $argv[1] ?? 'Hello, welcome to Morocco.';
$base = rtrim(getenv('TRANSLATOR_API_BASE') ?: 'http://localhost:8080/translator-service/api', '/');
$user = getenv('TRANSLATOR_USER') ?: 'DarijaTranslator';
$pass = getenv('TRANSLATOR_PASSWORD') ?: 'Morocco';

$url = $base . '/translator/translate';
$payload = json_encode(['text' => $text], JSON_THROW_ON_ERROR);

$ch = curl_init($url);
curl_setopt_array($ch, [
    CURLOPT_POST => true,
    CURLOPT_HTTPHEADER => [
        'Content-Type: application/json',
        'Authorization: Basic ' . base64_encode($user . ':' . $pass),
    ],
    CURLOPT_POSTFIELDS => $payload,
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_TIMEOUT => 120,
]);

$response = curl_exec($ch);
$code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$error = curl_error($ch);
curl_close($ch);

if ($response === false) {
    fwrite(STDERR, "cURL error: {$error}\n");
    exit(1);
}

$data = json_decode($response, true);
echo "HTTP {$code}\n";
if ($code >= 200 && $code < 300 && is_array($data)) {
    echo "Source: {$data['sourceText']}\n";
    echo "Darija: {$data['translatedText']}\n";
} else {
    echo $response . "\n";
    exit(1);
}
