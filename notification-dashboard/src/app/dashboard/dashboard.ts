import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClientModule, HttpErrorResponse } from '@angular/common/http';
import { NotificationService, Notification } from '../notification';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, HttpClientModule, FormsModule],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css']
})
export class DashboardComponent implements OnInit, OnDestroy {
  notifications: Notification[] = [];
  unreadCount: number = 0;
  userId: number = 1;
  message: string = '';
  idempotencyKey: string = '';
  logs: string[] = [];
  sseStatus: string = 'Disconnected';
  lastResponse: string = '';
  private eventSource!: EventSource;

  constructor(private notificationService: NotificationService) {}

  ngOnInit(): void {
    this.loadNotifications();
    this.loadUnreadCount();
    this.connectSSE();
  }

  log(msg: string): void {
    const time = new Date().toLocaleTimeString();
    this.logs.unshift(`[${time}] ${msg}`);
    if (this.logs.length > 20) this.logs.pop();
  }

  loadNotifications(): void {
    this.notificationService.getNotifications(this.userId).subscribe({
      next: data => {
        this.notifications = data;
        this.log(`Fetched ${data.length} notifications for user ${this.userId}`);
      },
      error: () => this.log('ERROR: Failed to fetch notifications')
    });
  }

  loadUnreadCount(): void {
    this.notificationService.getUnreadCount(this.userId).subscribe({
      next: count => {
        this.unreadCount = count;
        this.log(`Unread count from Redis: ${count}`);
      },
      error: () => this.log('ERROR: Failed to fetch unread count')
    });
  }

  connectSSE(): void {
    if (this.eventSource) this.eventSource.close();
    this.sseStatus = 'Connecting...';
    this.eventSource = this.notificationService.subscribeToSSE(this.userId);
    this.eventSource.onopen = () => {
      this.sseStatus = 'Connected';
      this.log('SSE connected — listening for live notifications');
    };
    this.eventSource.addEventListener('notification', (event: MessageEvent) => {
      const notification: Notification = JSON.parse(event.data);
      this.notifications.unshift(notification);
      this.unreadCount++;
      this.log(`LIVE: "${notification.message}" received via SSE`);
    });
    this.eventSource.onerror = () => {
      this.sseStatus = 'Disconnected';
      this.log('SSE disconnected');
    };
  }

  disconnectSSE(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.sseStatus = 'Disconnected';
      this.log('SSE manually disconnected');
    }
  }

  reconnectSSE(): void {
    this.log('Reconnecting SSE — unread notifications will be re-delivered...');
    this.connectSSE();
    this.loadNotifications();
  }

  createNotification(): void {
    if (!this.message) { this.log('ERROR: Message is required'); return; }
    const key = this.idempotencyKey || `key-${Date.now()}`;
    this.notificationService.create(this.userId, this.message, key).subscribe({
      next: n => {
        this.lastResponse = JSON.stringify(n, null, 2);
        this.log(`Created notification id=${n.id} key=${key}`);
        this.message = '';
        this.idempotencyKey = '';
      },
      error: (err: HttpErrorResponse) => {
        this.lastResponse = `${err.status}: ${err.error?.message || err.message}`;
        if (err.status === 429) this.log('RATE LIMIT: Too many requests — 429 returned');
        else if (err.status === 409) this.log('DUPLICATE: Idempotency key already used — rejected');
        else this.log(`ERROR ${err.status}: ${err.message}`);
      }
    });
  }

  sendDuplicate(): void {
    const key = 'fixed-demo-key-001';
    this.log(`Sending duplicate with key="${key}"...`);
    this.notificationService.create(this.userId, 'Duplicate test', key).subscribe({
      next: n => this.log(`First request OK id=${n.id}`),
      error: (err: HttpErrorResponse) => this.log(`DUPLICATE BLOCKED: ${err.status}`)
    });
    setTimeout(() => {
      this.notificationService.create(this.userId, 'Duplicate test', key).subscribe({
        next: () => this.log('WARNING: Duplicate was NOT blocked'),
        error: (err: HttpErrorResponse) => this.log(`DUPLICATE BLOCKED on 2nd request: ${err.status}`)
      });
    }, 500);
  }

  spamRequests(): void {
    this.log('Spamming 12 requests to trigger rate limit...');
    for (let i = 1; i <= 12; i++) {
      setTimeout(() => {
        this.notificationService.create(this.userId, `Spam ${i}`, `spam-${Date.now()}-${i}`).subscribe({
          next: n => this.log(`Request ${i}: OK id=${n.id}`),
          error: (err: HttpErrorResponse) => this.log(`Request ${i}: ${err.status === 429 ? 'RATE LIMITED 429' : 'ERROR ' + err.status}`)
        });
      }, i * 100);
    }
  }

  markAsRead(notification: Notification): void {
    if (!notification.read) {
      this.notificationService.markAsRead(notification.id).subscribe({
        next: () => {
          notification.read = true;
          this.unreadCount = Math.max(0, this.unreadCount - 1);
          this.log(`Marked notification id=${notification.id} as read. Unread: ${this.unreadCount}`);
        },
        error: () => this.log('ERROR: Failed to mark as read')
      });
    }
  }

  ngOnDestroy(): void {
    if (this.eventSource) this.eventSource.close();
  }
}