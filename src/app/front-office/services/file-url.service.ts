// src/app/front-office/services/file-url.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../front-office/environment/envirement';

export interface UploadResponse {
  fileUrl: string;
  fileName: string;
  fileSize: number;
  contentType: string;
  coursId?: number;
  chapitreId?: number;
  type?: string;
}

@Injectable({
  providedIn: 'root',
})
export class FileUrlService {
  private gatewayUrl = environment.apiGatewayUrl; // http://localhost:8082
  private uploadBasePath = environment.uploadBasePath; // /api/files

  constructor(private http: HttpClient) {}

  /**
   * Upload a file (requires coursId and chapitreId, optional contentBlockId)
   */
uploadFile(
  file: File,
  type: string,
  coursId: number,
  chapitreId: number,
  contentBlockId: number,
): Observable<UploadResponse> {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('type', type.toUpperCase());
  formData.append('coursId', coursId.toString());
  formData.append('courseId', coursId.toString());
  formData.append('chapitreId', chapitreId.toString());
  formData.append('chapterId', chapitreId.toString());
  formData.append('contentBlockId', contentBlockId.toString());

  // Fix: Use the correct path with /uploads
  const url = `${this.gatewayUrl}/api/courses/uploads/content`;
  console.log(
    'Uploading to:',
    url,
    'payload:',
    {
      type: type.toUpperCase(),
      coursId,
      courseId: coursId,
      chapitreId,
      chapterId: chapitreId,
      contentBlockId,
      fileName: file?.name,
      fileSize: file?.size,
    },
  );
  return this.http.post<UploadResponse>(url, formData);
}

  /**
   * Get thumbnail URL - requires coursId to match backend structure
   * Backend path: /api/courses/uploads/{coursId}/thumbnails/{filename}
   */
  getThumbnailUrl(filename: string, coursId?: number): string {
    // No filename provided - use SVG placeholder
    if (!filename) {
      return 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI2VlZSIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBmb250LXNpemU9IjI0IiBmaWxsPSIjYWFhIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBkeT0iLjNlbSI+Q291cnNlIENvdmVyPC90ZXh0Pjwvc3ZnPg==';
    }
    
    if (filename.startsWith('http')) return filename;

    // If it starts with /api, it's already a complete path from backend
    if (filename.startsWith('/api')) {
      return `${this.gatewayUrl}${filename}`;
    }

    // If it's an absolute file path (from backend), extract just the filename
    let extractedFilename = filename;
    if (filename.includes('\\') || filename.includes('/')) {
      const parts = filename.split(/[\\\/]/);;
      extractedFilename = parts[parts.length - 1];
    }

    // Extract coursId from filename if not provided (e.g., "1614234567_course.jpg" with coursId passed)
    let id = coursId;
    if (!id && extractedFilename) {
      // Try to extract from filename pattern like "1614234567_course.jpg"
      const match = extractedFilename.match(/^(\d+)\./);;
      if (match) {
        id = parseInt(match[1], 10);
      }
    }

    // Get file extension
    const extension = extractedFilename.includes('.') 
      ? extractedFilename.substring(extractedFilename.lastIndexOf('.')) 
      : '.jpg';

    // Return the file URL through the gateway using correct backend path
    // Path format: /api/courses/uploads/{coursId}/thumbnails/{filename}
    if (id && id > 0) {
      return `${this.gatewayUrl}/api/courses/uploads/${id}/thumbnails/${extractedFilename}`;
    }
    
    // Fallback if coursId is not available
    return 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI2VlZSIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBmb250LXNpemU9IjI0IiBmaWxsPSIjYWFhIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBkeT0iLjNlbSI+Q291cnNlIENvdmVyPC90ZXh0Pjwvc3ZnPg==';
  }

  /**
   * Get image URL
   */
  getImageUrl(filename: string): string {
    if (!filename) return '';
    if (filename.startsWith('http')) return filename;

    if (filename.includes('\\') || filename.includes('/')) {
      const parts = filename.split(/[\\\/]/);
      filename = parts[parts.length - 1];
    }

    return `${this.gatewayUrl}${this.uploadBasePath}/images/${filename}`;
  }

  /**
   * Get video URL
   */
  getVideoUrl(filename: string): string {
    if (!filename) return '';
    if (filename.startsWith('http')) return filename;

    if (filename.includes('\\') || filename.includes('/')) {
      const parts = filename.split(/[\\\/]/);
      filename = parts[parts.length - 1];
    }

    return `${this.gatewayUrl}${this.uploadBasePath}/videos/${filename}`;
  }

  /**
   * Get PDF URL
   */
  getPdfUrl(filename: string): string {
    if (!filename) return '';
    if (filename.startsWith('http')) return filename;

    if (filename.includes('\\') || filename.includes('/')) {
      const parts = filename.split(/[\\\/]/);
      filename = parts[parts.length - 1];
    }

    return `${this.gatewayUrl}${this.uploadBasePath}/pdfs/${filename}`;
  }

  /**
   * Get content URL for any subfolder
   */
  getContentUrl(subfolder: string, filename: string): string {
    if (!filename) return '';
    if (filename.startsWith('http')) return filename;

    if (filename.includes('\\') || filename.includes('/')) {
      const parts = filename.split(/[\\\/]/);
      filename = parts[parts.length - 1];
    }

    return `${this.gatewayUrl}${this.uploadBasePath}/${subfolder}/${filename}`;
  }

  /**
   * Get file URL from various formats - handles backend file paths
   */
  getFileUrl(filePath: string): string {
    if (!filePath) return '';

    // Already a complete URL
    if (filePath.startsWith('http')) return filePath;

    // Already starts with /api/courses/uploads - prepend gateway URL
    if (filePath.startsWith('/api/courses/uploads')) {
      return `${this.gatewayUrl}${filePath}`;
    }

    // If it's an absolute file path from backend (C:\...\uploads\...), keep relative structure after uploads/
    if (filePath.includes('\\') || filePath.match(/^[A-Z]:/)) {
      const normalizedPath = filePath.replace(/\\/g, '/');

      const uploadsSegment = '/uploads/';
      const uploadsIndex = normalizedPath.toLowerCase().indexOf(uploadsSegment);

      if (uploadsIndex >= 0) {
        const relativePath = normalizedPath.substring(uploadsIndex + uploadsSegment.length);
        return `${this.gatewayUrl}/api/courses/uploads/${relativePath}`;
      }

      const parts = normalizedPath.split('/');
      const filename = parts[parts.length - 1];
      if (normalizedPath.includes('thumbnail')) {
        return this.getThumbnailUrl(filename);
      }
      return `${this.gatewayUrl}/api/courses/uploads/content/${filename}`;
    }

    // If path starts with /api/files (legacy gateway route)
    if (filePath.startsWith('/api/files')) {
      return `${this.gatewayUrl}${filePath}`;
    }

    // If path starts with /uploads (legacy)
    if (filePath.startsWith('/uploads')) {
      const filename = filePath.split('/').pop();
      return `${this.gatewayUrl}/api/courses/uploads/content/${filename}`;
    }

    // Default: treat as filename
    return `${this.gatewayUrl}/api/courses/uploads/content/${filePath}`;
  }

  /**
   * Get URL for a file with course/chapter/content_block structure
   * This matches the backend file organization
   */
  getContentBlockFileUrl(
    coursId: number,
    chapitreId: number,
    contentBlockId: number,
    type: string,
    filename: string,
  ): string {
    if (!filename) return '';

    // If already a complete URL
    if (filename.startsWith('http')) return filename;

    // If it starts with /api, prepend gateway URL
    if (filename.startsWith('/api')) {
      return `${this.gatewayUrl}${filename}`;
    }

    // Extract just the filename if it's a full path
    if (filename.includes('\\') || filename.includes('/')) {
      const parts = filename.split(/[\\\/]/);
      filename = parts[parts.length - 1];
    }

    const typeFolder = this.mapTypeToFolder(type);
    // URL format: /api/files/cours_{id}/chapitre_{id}/content_block_{id}/{type}/{filename}
    return `${this.gatewayUrl}${this.uploadBasePath}/cours_${coursId}/chapitre_${chapitreId}/content_block_${contentBlockId}/${typeFolder}/${filename}`;
  }

  /**
   * Get URL for a file with course/chapter structure
   */
  getCourseFileUrl(
    coursId: number,
    chapitreId: number,
    type: string,
    filename: string,
  ): string {
    if (!filename) return 'assets/images/default-course.jpg';

    // If already a complete URL
    if (filename.startsWith('http')) return filename;

    // If it starts with /api, prepend gateway URL
    if (filename.startsWith('/api')) {
      return `${this.gatewayUrl}${filename}`;
    }

    // Extract just the filename if it's a full path
    if (filename.includes('\\') || filename.includes('/')) {
      const parts = filename.split(/[\\\/]/);
      filename = parts[parts.length - 1];
    }

    const typeFolder = this.mapTypeToFolder(type);
    return `${this.gatewayUrl}${this.uploadBasePath}/${typeFolder}/${filename}`;
  }

  /**
   * Helper method to map type to folder
   */
  private mapTypeToFolder(type: string): string {
    switch (type?.toUpperCase()) {
      case 'IMAGE':
        return 'images';
      case 'VIDEO':
        return 'videos';
      case 'PDF':
        return 'pdfs';
      case 'FILE':
        return 'files';
      default:
        return 'content';
    }
  }
}
