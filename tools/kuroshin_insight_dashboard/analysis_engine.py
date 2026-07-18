import os
# Kütüphaneyi en temel haliyle import ediyoruz.
import pygount
from collections import defaultdict

def validate_and_detect_project_type(path):
    """Verilen yolun geçerli bir proje olup olmadığını kontrol eder ve türünü tespit eder."""
    if not os.path.isdir(path):
        return "Unknown", "Belirtilen yol bir klasör değil."
    
    is_android = os.path.isdir(os.path.join(path, "app")) and \
                 (os.path.exists(os.path.join(path, "build.gradle.kts")) or \
                  os.path.exists(os.path.join(path, "build.gradle")))
    if is_android:
        return "Android (Kotlin/Java)", "Proje doğrulandı."
        
    is_unity = os.path.isdir(os.path.join(path, "Assets")) and \
               os.path.isdir(os.path.join(path, "ProjectSettings"))
    if is_unity:
        return "Unity (C#)", "Proje doğrulandı."
        
    return "Unknown", "Geçerli bir Unity veya Android projesi gibi görünmiyor. Lütfen ana proje klasörünü seçtiğinizden emin olun."

def analyze_loc(path):
    """
    Belirtilen yoldaki kaynak kodunu analiz eder ve dil başına kod satırı sayısını döndürür.
    """
    project_type, message = validate_and_detect_project_type(path)
    
    if project_type == "Unknown":
        return None, 0, {"error": message}, []

    language_stats = defaultdict(int)
    total_code_lines = 0
    skipped_files = [] # Okunamayan dosyaları saklamak için yeni bir liste

    try:
        for root, dirs, files in os.walk(path):
            # Analiz hızını artırmak için bilinen gereksiz klasörleri atla
            dirs[:] = [d for d in dirs if d not in ['.git', '.idea', 'build', 'venv', '__pycache__']]
            for file in files:
                file_path = os.path.join(root, file)
                if os.path.isfile(file_path):
                    try:
                        # Kütüphanenin en temel ve stabil fonksiyonunu kullanıyoruz.
                        analysis = pygount.SourceAnalysis.from_file(file_path, "utf-8")
                        if analysis.state == "ok":
                            language_stats[analysis.language] += analysis.code
                            total_code_lines += analysis.code
                        elif analysis.state != "binary":
                            # Eğer dosya binary değil ama yine de okunamıyorsa listeye ekle
                            skipped_files.append(file_path)
                    except Exception:
                        # Analiz edilemeyen dosyaları listeye ekle ve atla
                        skipped_files.append(file_path)
                        continue
    except Exception as e:
        return None, 0, {"error": f"Genel analiz hatası: {e}"}, []
            
    return project_type, total_code_lines, dict(language_stats), skipped_files