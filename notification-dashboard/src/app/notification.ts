import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Notification {
  id: number;
  userId: number;
  message: string;
  read: boolean;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private baseUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  getNotifications(userId: number): Observable<Notification[]> {
    return this.http.get<Notification[]>(`${this.baseUrl}/notifications/user/${userId}`);
  }

  getUnreadCount(userId: number): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/notifications/user/${userId}/unread-count`);
  }

  markAsRead(id: number): Observable<Notification> {
    return this.http.patch<Notification>(`${this.baseUrl}/notifications/${id}/read`, {});
  }

  subscribeToSSE(userId: number): EventSource {
    return new EventSource(`${this.baseUrl}/sse/subscribe/${userId}`);
  }

  create(userId: number, message: string, idempotencyKey: string): Observable<Notification> {
    return this.http.post<Notification>(`${this.baseUrl}/notifications`, { userId, message, idempotencyKey });
  }
}