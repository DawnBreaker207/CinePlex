import {
  Component,
  DestroyRef,
  effect,
  ElementRef,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { ChangeDetectionStrategy } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzAvatarModule } from 'ng-zorro-antd/avatar';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzFloatButtonModule } from 'ng-zorro-antd/float-button';
import { ChatboxService } from '@core/services/report/chatbox.service';
import { MarkdownComponent } from 'ngx-markdown';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

interface Message {
  content: string;
  sender: 'user' | 'bot';
  time: Date;
}

@Component({
  selector: 'app-chat',
  imports: [
    CommonModule,
    FormsModule,
    NzButtonModule,
    NzInputModule,
    NzAvatarModule,
    NzIconModule,
    NzFloatButtonModule,
    MarkdownComponent,
    TranslatePipe,
  ],
  templateUrl: './chat.component.html',
  styleUrl: './chat.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChatComponent {
  private chatboxService = inject(ChatboxService);
  private destroyRef = inject(DestroyRef);
  private translate = inject(TranslateService);
  scrollBottom = viewChild<ElementRef>('scrollBottom');

  isOpen = signal<boolean>(false);
  isTyping = signal<boolean>(false);
  inputValue = signal<string>('');
  messages = signal<Message[]>([
    {
      content: this.translate.instant('chat.welcome'),
      sender: 'bot',
      time: new Date(),
    },
  ]);

  constructor() {
    effect(() => {
      const msgs = this.messages();
      const open = this.isOpen();
      const typing = this.isTyping();
      if (open && (msgs.length > 0 || typing)) {
        setTimeout(() => this.scrollBottom(), 50);
      }
    });
  }

  toggleChat() {
    this.isOpen.update((v) => !v);
  }

  sendMessage() {
    const text = this.inputValue().trim();
    if (!text || this.isTyping()) return;

    this.messages.update((prev) => [
      ...prev,
      { content: text, sender: 'user', time: new Date() },
    ]);
    this.inputValue.set('');
    this.isTyping.set(true);

    this.chatboxService
      .sendMessage(text)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.messages.update((prev) => [
            ...prev,
            { content: res, sender: 'bot', time: new Date() },
          ]);
          this.isTyping.set(false);
        },
        error: (err) => {
          console.error(err);
          this.messages.update((prev) => [
            ...prev,
            {
              content: this.translate.instant('chat.busy'),
              sender: 'bot',
              time: new Date(),
            },
          ]);
          this.isTyping.set(false);
        },
      });
  }

  scrollToBottom() {
    const el = this.scrollBottom()?.nativeElement;
    if (el) {
      el.scrollTop = el.scrollHeight;
    }
  }
}
